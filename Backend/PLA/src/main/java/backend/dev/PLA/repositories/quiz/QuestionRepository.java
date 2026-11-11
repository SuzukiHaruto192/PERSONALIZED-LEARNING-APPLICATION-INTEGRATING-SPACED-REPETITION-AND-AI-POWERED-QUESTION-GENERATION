package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Question;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    long countByQuizId(Long quizId);

    // Mỗi phần tử: [quizId (Long), số câu hỏi (Long)]
    @Query("SELECT q.quiz.id, COUNT(q) FROM Question q WHERE q.quiz.id IN :quizIds GROUP BY q.quiz.id")
    List<Object[]> countByQuizIds(@Param("quizIds") Collection<Long> quizIds);

    long countByPassageId(Long passageId);

    boolean existsByPassageId(Long passageId);

    // Mỗi phần tử: [passageId (Long), số câu hỏi (Long)]
    @Query("SELECT q.passage.id, COUNT(q) FROM Question q WHERE q.passage.id IN :passageIds GROUP BY q.passage.id")
    List<Object[]> countByPassageIds(@Param("passageIds") Collection<Long> passageIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Question q WHERE q.quiz.id = :quizId")
    int deleteAllByQuizId(@Param("quizId") Long quizId);
}
