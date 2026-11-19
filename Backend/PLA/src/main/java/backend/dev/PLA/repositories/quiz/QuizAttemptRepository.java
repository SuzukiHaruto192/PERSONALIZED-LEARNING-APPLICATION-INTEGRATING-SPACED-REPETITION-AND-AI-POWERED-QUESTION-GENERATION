package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.QuizAttempt;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    boolean existsByQuizId(Long quizId);

    // Nạp sẵn quiz/skill để build response không bị N+1; dùng index idx_attempts_user (user_id, submitted_at DESC)
    @EntityGraph(attributePaths = {"quiz", "quiz.skill"})
    Page<QuizAttempt> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"quiz", "quiz.skill"})
    List<QuizAttempt> findByQuizIdAndUserIdOrderBySubmittedAtDescIdDesc(Long quizId, Long userId);
}
