package backend.dev.PLA.services.quiz;

import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.enums.QuizStatus;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.QuizRepository;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

/** Kiểm tra quyền trên quiz, dùng chung cho quiz và các bảng con (passages, questions, options). */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAccessService {
    QuizRepository quizRepository;

    /** Quiz tồn tại (404) và là của user hiện tại (403). Quiz hệ thống (created_by = null) cũng là 403. */
    public Quiz findViewableQuiz(Long userId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        checkOwner(quiz, userId);
        return quiz;
    }

    /**
     * Quiz tồn tại (404), là của user hiện tại (403) và không đang GENERATING (409). Khoá dòng tới hết transaction,
     * nên phải gọi trong một transaction ghi.
     */
    public Quiz findWritableQuiz(Long userId, Long quizId) {
        Quiz quiz =
                quizRepository.findByIdForUpdate(quizId).orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        checkOwner(quiz, userId);
        if (quiz.getStatus() == QuizStatus.GENERATING) {
            throw new AppException(ErrorCode.QUIZ_GENERATING);
        }
        return quiz;
    }

    /** Không phải người tạo quiz → 403 ACCESS_DENIED (dùng chung cho mọi thao tác xem và ghi). */
    public void checkOwner(Quiz quiz, Long userId) {
        if (!isOwner(quiz, userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    public boolean isOwner(Quiz quiz, Long userId) {
        return quiz.getCreatedBy() != null && Objects.equals(quiz.getCreatedBy().getId(), userId);
    }
}
