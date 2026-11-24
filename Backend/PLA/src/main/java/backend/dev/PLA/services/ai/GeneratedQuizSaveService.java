package backend.dev.PLA.services.ai;

import static backend.dev.PLA.services.ai.AiQuizRules.isBlank;
import static backend.dev.PLA.services.ai.AiQuizRules.nullToEmpty;

import backend.dev.PLA.dto.ai.GeneratedOption;
import backend.dev.PLA.dto.ai.GeneratedPassage;
import backend.dev.PLA.dto.ai.GeneratedQuestion;
import backend.dev.PLA.dto.ai.GeneratedQuiz;
import backend.dev.PLA.dto.request.Quiz.OptionRequest;
import backend.dev.PLA.dto.request.Quiz.PassageRequest;
import backend.dev.PLA.dto.request.Quiz.QuestionRequest;
import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.services.quiz.OptionService;
import backend.dev.PLA.services.quiz.PassageService;
import backend.dev.PLA.services.quiz.QuestionService;
import backend.dev.PLA.services.quiz.QuizService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chuyển quiz do AI sinh (đã qua QuizOutputValidator) sang các request DTO rồi lưu bằng các service CRUD có sẵn:
 * QuizService → PassageService → QuestionService → OptionService. Dùng lại toàn bộ quy tắc nghiệp vụ của chúng (quyền
 * sở hữu, khoá quiz, tự tính position, giới hạn số câu / số đáp án, một đáp án đúng...).
 *
 * <p>Cả quiz lưu trong một transaction: các service con join vào đây, lỗi ở bất kỳ bước nào thì rollback toàn bộ, không
 * để lại quiz dở dang. Transaction tách khỏi bước gọi LLM (chậm) nên không giữ connection DB lâu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeneratedQuizSaveService {
    QuizService quizService;
    PassageService passageService;
    QuestionService questionService;
    OptionService optionService;

    /**
     * Lưu quiz AI sinh, status = READY (do QuizService.createQuiz gán).
     *
     * @param request skillId / topicId / customTopic / formatId lấy từ request người dùng
     * @param difficulty độ khó đã tính từ trình độ + FSRS
     * @return id quiz vừa tạo
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(GeneratedQuiz generated, QuizRequest request, Long userId, short difficulty) {
        try {
            Long quizId = quizService
                    .createQuiz(userId, toQuizRequest(generated, request), difficulty)
                    .getId();

            // position để trống: các service tự xếp cuối (max + 1) theo đúng thứ tự AI trả về
            for (GeneratedPassage gp : nullToEmpty(generated.getPassages())) {
                Long passageId = passageService
                        .createPassage(userId, quizId, toPassageRequest(gp))
                        .getId();
                for (GeneratedQuestion gq : nullToEmpty(gp.getQuestions())) {
                    Long questionId = questionService
                            .createQuestion(userId, quizId, toQuestionRequest(gq, passageId))
                            .getId();
                    if (gq.getType() == QuestionType.SINGLE_CHOICE) {
                        for (GeneratedOption go : nullToEmpty(gq.getOptions())) {
                            optionService.createOption(userId, questionId, toOptionRequest(go));
                        }
                    }
                }
            }
            return quizId;
        } catch (DataIntegrityViolationException e) {
            // Đã qua validator nên hiếm khi xảy ra; nếu có thì rollback toàn bộ quiz
            log.error(
                    "Lưu quiz AI sinh vi phạm ràng buộc DB: {}",
                    e.getMostSpecificCause().getMessage());
            throw new AppException(ErrorCode.AI_QUIZ_SAVE_FAILED);
        }
    }

    // ===================== AI output → request DTO =====================

    /** Giữ title / skill / topic / format của người dùng; description lấy từ AI (AI không trả thì dùng của người dùng). */
    private QuizRequest toQuizRequest(GeneratedQuiz generated, QuizRequest request) {
        return QuizRequest.builder()
                .title(request.getTitle())
                .description(
                        isBlank(generated.getDescription()) ? request.getDescription() : generated.getDescription())
                .skillId(request.getSkillId())
                .topicId(request.getTopicId())
                .customTopic(request.getCustomTopic())
                .formatId(request.getFormatId())
                .build();
    }

    private PassageRequest toPassageRequest(GeneratedPassage gp) {
        return PassageRequest.builder().content(gp.getContent()).build();
    }

    private QuestionRequest toQuestionRequest(GeneratedQuestion gq, Long passageId) {
        return QuestionRequest.builder()
                .passageId(passageId)
                .type(gq.getType())
                .content(gq.getContent())
                // SINGLE_CHOICE không được có correctText (AI có thể trả "" thay vì null)
                .correctText(gq.getType() == QuestionType.FILL_BLANK ? gq.getCorrectText() : null)
                .explanation(gq.getExplanation())
                .build();
    }

    private OptionRequest toOptionRequest(GeneratedOption go) {
        return OptionRequest.builder()
                .content(go.getContent())
                .isCorrect(Boolean.TRUE.equals(go.getIsCorrect()))
                .build();
    }
}
