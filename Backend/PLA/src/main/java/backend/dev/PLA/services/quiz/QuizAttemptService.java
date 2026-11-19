package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.AttemptAnswerRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptSearchRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.AttemptResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.dto.response.quiz.QuizAttemptHistoryResponse;
import backend.dev.PLA.dto.response.quiz.QuizResponse;
import backend.dev.PLA.entities.quiz.AttemptAnswer;
import backend.dev.PLA.entities.quiz.Option;
import backend.dev.PLA.entities.quiz.Question;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.QuizAttempt;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.enums.QuizStatus;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.AttemptMapper;
import backend.dev.PLA.mapper.QuestionMapper;
import backend.dev.PLA.repositories.quiz.AttemptAnswerRepository;
import backend.dev.PLA.repositories.quiz.QuestionRepository;
import backend.dev.PLA.repositories.quiz.QuizAttemptRepository;
import backend.dev.PLA.repositories.quiz.QuizRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lịch sử làm bài quiz của user hiện tại: nộp bài (chấm điểm + lưu), xem tất cả lượt làm, xem lượt làm theo quiz. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptService {
    QuizAttemptRepository quizAttemptRepository;
    AttemptAnswerRepository attemptAnswerRepository;
    QuizRepository quizRepository;
    QuestionRepository questionRepository;
    UserRepository userRepository;
    QuizAccessService quizAccessService;
    QuizService quizService;
    TopicProgressService topicProgressService;

    @Transactional(readOnly = true)
    public PageResponse<AttemptResponse> getMyAttempts(Long userId, AttemptSearchRequest filter) {
        PageRequest pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId, pageable);
        return PageResponse.of(attempts, AttemptMapper::toAttemptResponse);
    }

    @Transactional(readOnly = true)
    public QuizAttemptHistoryResponse getQuizAttempts(Long userId, Long quizId) {
        QuizResponse quiz = quizService.getQuiz(userId, quizId);

        List<QuizAttempt> attempts =
                quizAttemptRepository.findByQuizIdAndUserIdOrderBySubmittedAtDescIdDesc(quizId, userId);
        Map<Long, List<AttemptAnswer>> answersByAttempt = new HashMap<>();
        if (!attempts.isEmpty()) {
            List<Long> ids = attempts.stream().map(QuizAttempt::getId).toList();
            answersByAttempt = attemptAnswerRepository.findByAttemptIds(ids).stream()
                    .collect(Collectors.groupingBy(a -> a.getId().getAttemptId(), Collectors.toList()));
        }

        List<QuestionResponse> questions = quiz.getQuestions();
        Map<Long, List<AttemptAnswer>> finalAnswers = answersByAttempt;
        return QuizAttemptHistoryResponse.builder()
                .quiz(quiz)
                .attempts(attempts.stream()
                        .map(attempt -> AttemptMapper.toAttemptHistoryResponse(
                                attempt, questions, finalAnswers.getOrDefault(attempt.getId(), List.of())))
                        .toList())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AttemptResponse submitAttempt(Long userId, Long quizId, AttemptRequest request) {
        Quiz quiz =
                quizRepository.findByIdForShare(quizId).orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        quizAccessService.checkOwner(quiz, userId);
        if (quiz.getStatus() != QuizStatus.READY) {
            throw new AppException(ErrorCode.QUIZ_NOT_READY);
        }

        List<Question> questions = questionRepository.findDetailsByQuizId(quizId);
        if (questions.isEmpty()) {
            throw new AppException(ErrorCode.QUIZ_HAS_NO_QUESTIONS);
        }
        Map<Long, Question> questionById = new HashMap<>();
        for (Question question : questions) {
            questionById.put(question.getId(), question);
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(userRepository.getReferenceById(userId))
                .totalQuestions(questions.size())
                .attemptAnswers(new ArrayList<>())
                .build();

        Set<Long> answeredIds = new HashSet<>();
        int correctCount = 0;
        for (AttemptAnswerRequest answerRequest : request.getAnswers()) {
            Question question = questionById.get(answerRequest.getQuestionId());
            if (question == null) {
                throw new AppException(ErrorCode.ATTEMPT_QUESTION_NOT_IN_QUIZ);
            }
            if (!answeredIds.add(question.getId())) {
                throw new AppException(ErrorCode.ATTEMPT_QUESTION_DUPLICATED);
            }
            AttemptAnswer answer = gradeAnswer(attempt, question, answerRequest);
            if (answer.isCorrect()) {
                correctCount++;
            }
            attempt.getAttemptAnswers().add(answer);
        }
        attempt.setCorrectCount(correctCount);

        quizAttemptRepository.save(attempt);
        attempt.setSubmittedAt(attempt.getStartedAt());
        quizAttemptRepository.flush();

        // Cập nhật lịch ôn FSRS của cặp (skill, topic) của quiz cho user nộp bài (chỉ lưu, không trả về)
        topicProgressService.recordAttempt(
                userId, quiz, correctCount, attempt.getTotalQuestions(), attempt.getSubmittedAt());

        List<QuestionResponse> questionResponses =
                questions.stream().map(QuestionMapper::toQuestionResponse).toList();
        return AttemptMapper.toAttemptHistoryResponse(attempt, questionResponses, attempt.getAttemptAnswers());
    }

    // ===== Helpers =====

    /**
     * SINGLE_CHOICE: đúng khi option được chọn có is_correct. FILL_BLANK: so với correct_text, không phân biệt hoa
     * thường và khoảng trắng thừa. Không chọn / không điền = bỏ qua câu, tính sai.
     */
    private AttemptAnswer gradeAnswer(QuizAttempt attempt, Question question, AttemptAnswerRequest request) {
        String answerText = trimToNull(request.getAnswerText());
        Option selected = null;
        boolean correct;

        if (question.getType() == QuestionType.FILL_BLANK) {
            if (request.getOptionId() != null) {
                throw new AppException(ErrorCode.ATTEMPT_ANSWER_TYPE_MISMATCH);
            }
            correct = answerText != null
                    && question.getCorrectText() != null
                    && normalize(answerText).equals(normalize(question.getCorrectText()));
        } else {
            if (answerText != null) {
                throw new AppException(ErrorCode.ATTEMPT_ANSWER_TYPE_MISMATCH);
            }
            if (request.getOptionId() != null) {
                List<Option> options = question.getOptions() == null ? List.of() : question.getOptions();
                selected = options.stream()
                        .filter(o -> Objects.equals(o.getId(), request.getOptionId()))
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_OPTION_NOT_IN_QUESTION));
            }
            correct = selected != null && selected.isCorrect();
        }

        return AttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .selectedOption(selected)
                .answerText(answerText)
                .isCorrect(correct)
                .build();
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
