package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.AttemptAnswer;
import backend.dev.PLA.entities.quiz.AttemptAnswerId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, AttemptAnswerId> {

    // Câu trả lời của nhiều lượt làm trong một query, sắp theo position câu hỏi
    @Query("""
            SELECT a FROM AttemptAnswer a
            JOIN a.question q
            WHERE a.attempt.id IN :attemptIds
            ORDER BY q.position
            """)
    List<AttemptAnswer> findByAttemptIds(@Param("attemptIds") Collection<Long> attemptIds);
}
