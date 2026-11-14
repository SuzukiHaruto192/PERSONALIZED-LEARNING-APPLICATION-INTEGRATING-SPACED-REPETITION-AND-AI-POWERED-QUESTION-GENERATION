package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.QuestionRequest;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.entities.quiz.Passage;
import backend.dev.PLA.entities.quiz.Question;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.QuestionMapper;
import backend.dev.PLA.repositories.quiz.OptionRepository;
import backend.dev.PLA.repositories.quiz.PassageRepository;
import backend.dev.PLA.repositories.quiz.QuestionRepository;
import backend.dev.PLA.repositories.quiz.QuizAttemptRepository;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionService {
    public static final int MAX_QUESTIONS_PER_QUIZ = 100;
    static final String HAS_ATTEMPTS_UPDATE_MESSAGE = "Quiz đã có lượt làm bài, không thể đổi loại câu hoặc đáp án";

    QuestionRepository questionRepository;
    OptionRepository optionRepository;
    PassageRepository passageRepository;
    TopicRepository topicRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizAccessService quizAccessService;

    // Mọi thao tác ghi đều khoá dòng quiz (findWritableQuiz) nên đếm số câu / tính max + 1 / kiểm tra trùng
    // position không bị request song song chen vào

    @Transactional(rollbackFor = Exception.class)
    public QuestionResponse createQuestion(Long userId, Long quizId, QuestionRequest request) {
        Quiz quiz = quizAccessService.findWritableQuiz(userId, quizId);
        if (questionRepository.countByQuizId(quizId) >= MAX_QUESTIONS_PER_QUIZ) {
            throw new AppException(ErrorCode.QUIZ_QUESTION_LIMIT);
        }

        short position;
        if (request.getPosition() == null) {
            int max = questionRepository.findMaxPositionByQuizId(quizId);
            if (max >= Short.MAX_VALUE) {
                throw new AppException(ErrorCode.QUESTION_POSITION_INVALID);
            }
            position = (short) (max + 1);
        } else {
            position = request.getPosition().shortValue();
            if (questionRepository.existsByQuizIdAndPosition(quizId, position)) {
                throw new AppException(ErrorCode.QUESTION_POSITION_DUPLICATED);
            }
        }

        Question question = Question.builder()
                .quiz(quiz)
                .position(position)
                .options(new ArrayList<>())
                .build();
        QuestionMapper.updateQuestion(
                question,
                request,
                resolvePassage(quizId, request.getPassageId()),
                resolveGrammarTopic(request.getGrammarTopicId()));
        return QuestionMapper.toQuestionResponse(questionRepository.saveAndFlush(question));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(Long userId, Long quizId, Long passageId) {
        quizAccessService.findViewableQuiz(userId, quizId);
        if (passageId == null) {
            return getQuestionResponses(quizId);
        }
        if (!passageRepository.existsByIdAndQuizId(passageId, quizId)) {
            throw new AppException(ErrorCode.PASSAGE_NOT_IN_QUIZ);
        }
        return questionRepository.findDetailsByQuizIdAndPassageId(quizId, passageId).stream()
                .map(QuestionMapper::toQuestionResponse)
                .toList();
    }

    /** Câu hỏi của quiz sắp theo position, kèm options. Không kiểm tra quyền: caller phải kiểm tra trước. */
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionResponses(Long quizId) {
        return questionRepository.findDetailsByQuizId(quizId).stream()
                .map(QuestionMapper::toQuestionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(Long userId, Long questionId) {
        Question question = questionRepository
                .findDetailsById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        quizAccessService.checkOwner(question.getQuiz(), userId);
        return QuestionMapper.toQuestionResponse(question);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionResponse updateQuestion(Long userId, Long questionId, QuestionRequest request) {
        Question question = findWritableQuestion(userId, questionId);
        Long quizId = question.getQuiz().getId();

        boolean typeChanged = request.getType() != question.getType();
        boolean correctTextChanged = !Objects.equals(trimToNull(request.getCorrectText()), question.getCorrectText());
        if ((typeChanged || correctTextChanged) && quizAttemptRepository.existsByQuizId(quizId)) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS, HAS_ATTEMPTS_UPDATE_MESSAGE);
        }
        if (typeChanged
                && request.getType() == QuestionType.FILL_BLANK
                && optionRepository.existsByQuestionId(questionId)) {
            throw new AppException(ErrorCode.QUESTION_HAS_OPTIONS);
        }

        // position bỏ trống thì giữ nguyên vị trí cũ
        if (request.getPosition() != null) {
            short position = request.getPosition().shortValue();
            if (questionRepository.existsByQuizIdAndPositionAndIdNot(quizId, position, questionId)) {
                throw new AppException(ErrorCode.QUESTION_POSITION_DUPLICATED);
            }
            question.setPosition(position);
        }
        QuestionMapper.updateQuestion(
                question,
                request,
                resolvePassage(quizId, request.getPassageId()),
                resolveGrammarTopic(request.getGrammarTopicId()));
        return QuestionMapper.toQuestionResponse(questionRepository.saveAndFlush(question));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long userId, Long questionId) {
        Question question = findWritableQuestion(userId, questionId);
        if (quizAttemptRepository.existsByQuizId(question.getQuiz().getId())) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS);
        }
        // Không đánh lại position các câu còn lại
        optionRepository.deleteByQuestionId(questionId);
        questionRepository.deleteByIdDirectly(questionId);
    }

    // ===== Helpers =====

    /** Câu hỏi tồn tại (404) và quiz cha ghi được (403 / 409, khoá dòng quiz). */
    private Question findWritableQuestion(Long userId, Long questionId) {
        Question question = questionRepository
                .findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        quizAccessService.findWritableQuiz(userId, question.getQuiz().getId());
        return question;
    }

    // Kiểm tra ở service thay vì chờ lỗi FK kép (passage_id, quiz_id) của database
    private Passage resolvePassage(Long quizId, Long passageId) {
        if (passageId == null) return null;
        return passageRepository
                .findByIdAndQuizId(passageId, quizId)
                .orElseThrow(() -> new AppException(ErrorCode.PASSAGE_NOT_IN_QUIZ));
    }

    private Topic resolveGrammarTopic(Long topicId) {
        if (topicId == null) return null;
        return topicRepository
                .findByIdAndGroupType(topicId, TopicGroupType.GRAMMAR)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GRAMMAR_TOPIC_INVALID));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
