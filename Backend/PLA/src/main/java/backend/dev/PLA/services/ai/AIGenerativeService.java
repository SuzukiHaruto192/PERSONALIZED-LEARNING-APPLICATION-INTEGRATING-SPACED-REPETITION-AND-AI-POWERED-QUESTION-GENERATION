package backend.dev.PLA.services.ai;

import static backend.dev.PLA.services.ai.AiQuizRules.MAX_ATTEMPTS;

import backend.dev.PLA.dto.ai.GeneratedQuiz;
import backend.dev.PLA.dto.ai.QuizPromptInput;
import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import backend.dev.PLA.dto.response.quiz.QuizResponse;
import backend.dev.PLA.entities.quiz.QuizFormat;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.UserTopicProgress;
import backend.dev.PLA.entities.quiz.UserTopicProgressId;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.AiOutputInvalidException;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.QuizFormatRepository;
import backend.dev.PLA.repositories.quiz.SkillRepository;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import backend.dev.PLA.repositories.quiz.UserTopicProgressRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.services.quiz.QuizService;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/**
 * Luồng chính sinh quiz bằng AI: lấy dữ liệu → dựng prompt (QuizPromptService) → gọi LLM → kiểm tra
 * (QuizOutputValidator) → sai thì gửi lỗi cho LLM làm lại → lưu DB (GeneratedQuizSaveService) → trả QuizResponse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIGenerativeService {
    static final int CUSTOM_TOPIC_MAX_LENGTH = 200;

    // Bean khai báo trong AiConfig, đã gắn sẵn system prompt
    ChatClient chatClient;
    QuizPromptService quizPromptService;
    QuizOutputValidator quizOutputValidator;
    GeneratedQuizSaveService generatedQuizSaveService;
    QuizService quizService;
    SkillRepository skillRepository;
    TopicRepository topicRepository;
    QuizFormatRepository quizFormatRepository;
    UserTopicProgressRepository userTopicProgressRepository;
    UserRepository userRepository;

    /**
     * Gọi LLM sinh quiz cá nhân hoá theo trình độ và độ nhớ của người dùng với (skill, topic). Quiz hợp lệ được lưu
     * vào DB (status READY) rồi trả về chi tiết quiz vừa lưu, có đủ id.
     *
     * <p>Không đặt @Transactional: gọi LLM mất vài chục giây, không giữ connection DB suốt thời gian đó. Các truy vấn
     * chỉ đọc cột thường (không chạm quan hệ LAZY) nên chạy được ngoài transaction.
     */
    public QuizResponse generateQuizByAI(QuizRequest request, Long userId) {
        // B0: kiểm tra request trước khi đọc DB / gọi LLM, tránh tốn lượt gọi Gemini cho request chắc chắn lưu lỗi
        validateRequest(request);

        // B1: lấy trình độ người dùng (users.current_score), skill / topic / format và biến FSRS từ
        // user_topic_progress. Độ khó KHÔNG lấy từ request mà tính từ hai nguồn này.
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Skill skill = findSkill(request.getSkillId());
        Topic topic = findTopic(request.getTopicId(), skill.getId());
        QuizFormat format = findFormat(request.getFormatId(), skill.getId());
        UserTopicProgress progress = findProgress(userId, skill, topic);

        short difficulty = QuizPromptService.computeDifficulty(user.getCurrentScore(), progress);
        boolean needsPassage = AiQuizRules.needsPassage(skill.getCode());

        // B2 + B3: prompt sinh quiz + ràng buộc định dạng đầu ra
        QuizPromptInput promptInput = quizPromptService.toPromptInput(
                request, user, skill, topic, format, progress, difficulty, needsPassage);
        String prompt = quizPromptService.buildPrompt(promptInput);

        // B4: gửi LLM, sai chuẩn thì làm lại
        GeneratedQuiz quiz = callUntilValid(prompt, request.getQuestionCount());

        // B5: hợp lệ → lưu DB (transaction ngắn) rồi trả chi tiết quiz vừa lưu, có đủ id
        Long quizId = generatedQuizSaveService.save(quiz, request, userId, difficulty);
        return quizService.getQuiz(userId, quizId);
    }

    /**
     * Gọi LLM tối đa MAX_ATTEMPTS lần. Mỗi lần sai chuẩn: đưa câu trả lời cũ + danh sách lỗi vào context (để LLM biết
     * mình đã trả gì và sai ở đâu) rồi yêu cầu làm lại.
     */
    private GeneratedQuiz callUntilValid(String prompt, int questionCount) {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(prompt));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String content = callLlm(messages);
            try {
                return quizOutputValidator.validate(content, questionCount);
            } catch (AiOutputInvalidException e) {
                log.warn("LLM trả quiz sai định dạng (lần {}/{}): {}", attempt, MAX_ATTEMPTS, e.getErrors());
                messages.add(new AssistantMessage(content == null ? "" : content));
                messages.add(new UserMessage(quizPromptService.buildRetryPrompt(e.getErrors())));
            }
        }
        throw new AppException(ErrorCode.AI_GENERATION_FAILED);
    }

    /**
     * Một lần gọi Gemini. Lỗi phía AI (429 hết quota, 5xx, timeout, sai API key...) do Spring AI ném ra là
     * RuntimeException; đổi thành AI_SERVICE_UNAVAILABLE (503) thay vì rơi xuống handler chung thành 500. Không thử lại
     * vì lỗi này thường không tự hết ngay, thử lại chỉ làm request treo lâu hơn.
     */
    private String callLlm(List<Message> messages) {
        try {
            return chatClient.prompt().messages(messages).call().content();
        } catch (RuntimeException e) {
            log.error("Gọi Gemini thất bại: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Cùng quy tắc với annotation trong QuizRequest (service không chạy @Valid nên kiểm tra lại ở đây, phòng khi
     * được gọi không qua controller). Bỏ qua difficulty vì độ khó do hệ thống tính. questionCount chỉ kiểm tra
     * bằng annotation ở controller.
     */
    private void validateRequest(QuizRequest request) {
        if (request.getSkillId() == null) {
            throw new AppException(ErrorCode.SKILL_BLANK);
        }
        if (isBlank(request.getTitle())) {
            throw new AppException(ErrorCode.QUIZ_TITLE_BLANK);
        }
        if (request.getTitle().trim().length() > AiQuizRules.TITLE_MAX_LENGTH) {
            throw new AppException(ErrorCode.QUIZ_TITLE_SIZE);
        }
        // chk_quiz_topic: đúng một trong topicId hoặc customTopic
        boolean hasCustomTopic = !isBlank(request.getCustomTopic());
        if ((request.getTopicId() != null) == hasCustomTopic) {
            throw new AppException(ErrorCode.QUIZ_TOPIC_EXACTLY_ONE);
        }
        // quizzes.custom_topic VARCHAR(200)
        if (hasCustomTopic && request.getCustomTopic().trim().length() > CUSTOM_TOPIC_MAX_LENGTH) {
            throw new AppException(ErrorCode.CUSTOM_TOPIC_SIZE);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private Skill findSkill(Short skillId) {
        return skillRepository.findById(skillId).orElseThrow(() -> new AppException(ErrorCode.QUIZ_SKILL_INVALID));
    }

    private Topic findTopic(Long topicId, Short skillId) {
        if (topicId == null) return null;
        return topicRepository
                .findByIdAndSkillId(topicId, skillId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_SKILL_TOPIC_MISMATCH));
    }

    private QuizFormat findFormat(Short formatId, Short skillId) {
        if (formatId == null) return null;
        return quizFormatRepository
                .findByIdAndSkillId(formatId, skillId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_FORMAT_INVALID));
    }

    // customTopic không có progress; topic chưa từng làm cũng chưa có dòng progress → null (người học mới)
    private UserTopicProgress findProgress(Long userId, Skill skill, Topic topic) {
        if (topic == null) return null;
        return userTopicProgressRepository
                .findById(new UserTopicProgressId(userId, skill.getId(), topic.getId()))
                .orElse(null);
    }
}
