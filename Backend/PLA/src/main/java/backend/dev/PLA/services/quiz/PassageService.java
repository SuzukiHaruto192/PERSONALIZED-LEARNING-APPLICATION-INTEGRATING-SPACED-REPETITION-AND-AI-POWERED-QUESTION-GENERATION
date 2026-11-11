package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.PassageRequest;
import backend.dev.PLA.dto.response.quiz.PassageResponse;
import backend.dev.PLA.entities.quiz.Passage;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.PassageMapper;
import backend.dev.PLA.repositories.quiz.PassageRepository;
import backend.dev.PLA.repositories.quiz.QuestionRepository;
import backend.dev.PLA.repositories.quiz.QuizAttemptRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PassageService {
    PassageRepository passageRepository;
    QuestionRepository questionRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizAccessService quizAccessService;

    // Mọi thao tác ghi đều khoá dòng quiz (findWritableQuiz) nên tính max + 1 / kiểm tra trùng position không bị
    // request song song chen vào

    @Transactional(rollbackFor = Exception.class)
    public PassageResponse createPassage(Long userId, Long quizId, PassageRequest request) {
        Quiz quiz = quizAccessService.findWritableQuiz(userId, quizId);

        short position;
        if (request.getPosition() == null) {
            position = (short) (passageRepository.findMaxPositionByQuizId(quizId) + 1);
        } else {
            position = request.getPosition().shortValue();
            if (passageRepository.existsByQuizIdAndPosition(quizId, position)) {
                throw new AppException(ErrorCode.PASSAGE_POSITION_DUPLICATED);
            }
        }

        Passage passage = Passage.builder().quiz(quiz).position(position).build();
        PassageMapper.updatePassage(passage, request);
        return PassageMapper.toPassageResponse(passageRepository.saveAndFlush(passage), 0);
    }

    @Transactional(readOnly = true)
    public List<PassageResponse> getPassages(Long userId, Long quizId) {
        quizAccessService.findViewableQuiz(userId, quizId);
        return getPassageResponses(quizId);
    }

    /** Passage của quiz sắp theo position, kèm số câu hỏi. Không kiểm tra quyền: caller phải kiểm tra trước. */
    @Transactional(readOnly = true)
    public List<PassageResponse> getPassageResponses(Long quizId) {
        List<Passage> passages = passageRepository.findByQuizIdOrderByPositionAsc(quizId);
        if (passages.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> questionCounts = new HashMap<>();
        List<Long> ids = passages.stream().map(Passage::getId).toList();
        for (Object[] row : questionRepository.countByPassageIds(ids)) {
            questionCounts.put((Long) row[0], (Long) row[1]);
        }
        return passages.stream()
                .map(p -> PassageMapper.toPassageResponse(p, questionCounts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PassageResponse getPassage(Long userId, Long passageId) {
        Passage passage =
                passageRepository.findById(passageId).orElseThrow(() -> new AppException(ErrorCode.PASSAGE_NOT_FOUND));
        quizAccessService.checkOwner(passage.getQuiz(), userId);
        return PassageMapper.toPassageResponse(passage, questionRepository.countByPassageId(passageId));
    }

    @Transactional(rollbackFor = Exception.class)
    public PassageResponse updatePassage(Long userId, Long passageId, PassageRequest request) {
        Passage passage = findWritablePassage(userId, passageId);
        Long quizId = passage.getQuiz().getId();

        // position bỏ trống thì giữ nguyên vị trí cũ
        if (request.getPosition() != null) {
            short position = request.getPosition().shortValue();
            if (passageRepository.existsByQuizIdAndPositionAndIdNot(quizId, position, passageId)) {
                throw new AppException(ErrorCode.PASSAGE_POSITION_DUPLICATED);
            }
            passage.setPosition(position);
        }
        PassageMapper.updatePassage(passage, request);
        return PassageMapper.toPassageResponse(
                passageRepository.saveAndFlush(passage), questionRepository.countByPassageId(passageId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePassage(Long userId, Long passageId) {
        Passage passage = findWritablePassage(userId, passageId);
        if (quizAttemptRepository.existsByQuizId(passage.getQuiz().getId())) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS);
        }
        if (questionRepository.existsByPassageId(passageId)) {
            throw new AppException(ErrorCode.PASSAGE_HAS_QUESTIONS);
        }
        passageRepository.detachFlashcardsByPassageId(passageId);
        passageRepository.deleteByIdDirectly(passageId);
    }

    /** Passage tồn tại (404) và quiz cha ghi được (403 / 409, khoá dòng quiz). */
    private Passage findWritablePassage(Long userId, Long passageId) {
        Passage passage =
                passageRepository.findById(passageId).orElseThrow(() -> new AppException(ErrorCode.PASSAGE_NOT_FOUND));
        quizAccessService.findWritableQuiz(userId, passage.getQuiz().getId());
        return passage;
    }
}
