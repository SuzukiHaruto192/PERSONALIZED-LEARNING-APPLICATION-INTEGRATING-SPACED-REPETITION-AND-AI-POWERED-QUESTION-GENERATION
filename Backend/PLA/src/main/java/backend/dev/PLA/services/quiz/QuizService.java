package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import backend.dev.PLA.dto.request.Quiz.QuizSearchRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.QuizResponse;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.QuizFormat;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.enums.QuizStatus;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.QuizMapper;
import backend.dev.PLA.repositories.quiz.OptionRepository;
import backend.dev.PLA.repositories.quiz.PassageRepository;
import backend.dev.PLA.repositories.quiz.QuestionRepository;
import backend.dev.PLA.repositories.quiz.QuizAttemptRepository;
import backend.dev.PLA.repositories.quiz.QuizFormatRepository;
import backend.dev.PLA.repositories.quiz.QuizRepository;
import backend.dev.PLA.repositories.quiz.SkillRepository;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizService {
    static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "title", "difficulty");

    QuizRepository quizRepository;
    QuestionRepository questionRepository;
    OptionRepository optionRepository;
    PassageRepository passageRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizFormatRepository quizFormatRepository;
    SkillRepository skillRepository;
    TopicRepository topicRepository;
    UserRepository userRepository;
    QuizAccessService quizAccessService;
    PassageService passageService;

    @Transactional(rollbackFor = Exception.class)
    public QuizResponse createQuiz(Long userId, QuizRequest request) {
        Quiz quiz = Quiz.builder()
                .createdBy(userRepository.getReferenceById(userId))
                .status(QuizStatus.READY)
                .build();
        applyRequest(quiz, request);
        return QuizMapper.toQuizResponse(quizRepository.saveAndFlush(quiz), 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizResponse> getMyQuizzes(Long userId, QuizSearchRequest filter) {
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getSize(), parseSort(filter.getSort()));
        Page<Quiz> quizzes = quizRepository.findAll(buildSpecification(userId, filter), pageable);

        Map<Long, Long> questionCounts = new HashMap<>();
        if (quizzes.hasContent()) {
            List<Long> ids = quizzes.getContent().stream().map(Quiz::getId).toList();
            for (Object[] row : questionRepository.countByQuizIds(ids)) {
                questionCounts.put((Long) row[0], (Long) row[1]);
            }
        }
        return PageResponse.of(
                quizzes, quiz -> QuizMapper.toQuizResponse(quiz, questionCounts.getOrDefault(quiz.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long userId, Long quizId) {
        Quiz quiz =
                quizRepository.findWithRefsById(quizId).orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        quizAccessService.checkOwner(quiz, userId);
        QuizResponse response = QuizMapper.toQuizResponse(quiz, questionRepository.countByQuizId(quizId));
        response.setPassages(passageService.getPassageResponses(quizId));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public QuizResponse updateQuiz(Long userId, Long quizId, QuizRequest request) {
        Quiz quiz = quizAccessService.findWritableQuiz(userId, quizId);
        applyRequest(quiz, request);
        return QuizMapper.toQuizResponse(quizRepository.saveAndFlush(quiz), questionRepository.countByQuizId(quizId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteQuiz(Long userId, Long quizId) {
        quizAccessService.findWritableQuiz(userId, quizId);
        if (quizAttemptRepository.existsByQuizId(quizId)) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS);
        }
        // Thứ tự theo khoá ngoại: options -> questions -> passages (bỏ liên kết flashcard) -> quiz
        optionRepository.deleteAllByQuizId(quizId);
        questionRepository.deleteAllByQuizId(quizId);
        passageRepository.detachFlashcardsByQuizId(quizId);
        passageRepository.deleteAllByQuizId(quizId);
        quizRepository.deleteById(quizId);
        quizRepository.flush();
    }

    // ===== Helpers =====

    private void applyRequest(Quiz quiz, QuizRequest request) {
        Skill skill = skillRepository
                .findById(request.getSkillId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_SKILL_INVALID));

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository
                    .findByIdAndSkillId(request.getTopicId(), skill.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.QUIZ_SKILL_TOPIC_MISMATCH));
        }

        QuizFormat format = null;
        if (request.getFormatId() != null) {
            format = quizFormatRepository
                    .findByIdAndSkillId(request.getFormatId(), skill.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.QUIZ_FORMAT_INVALID));
        }

        QuizMapper.updateQuiz(quiz, request, skill, topic, format);
    }

    private Specification<Quiz> buildSpecification(Long userId, QuizSearchRequest filter) {
        Specification<Quiz> spec =
                (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId);
        if (filter.getSkillId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("skill").get("id"), filter.getSkillId()));
        }
        if (filter.getTopicId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("topic").get("id"), filter.getTopicId()));
        }
        if (filter.getFormatId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("format").get("id"), filter.getFormatId()));
        }
        if (filter.getDifficulty() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), filter.getDifficulty()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            String pattern = "%" + escapeLike(filter.getKeyword().trim().toLowerCase()) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        return spec;
    }

    // sort dạng "field" hoặc "field,asc|desc"; thêm id để thứ tự ổn định khi phân trang
    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field) || parts.length > 2) {
            throw new AppException(ErrorCode.QUIZ_SORT_INVALID);
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> new AppException(ErrorCode.QUIZ_SORT_INVALID));
        }
        return Sort.by(direction, field).and(Sort.by(direction, "id"));
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
