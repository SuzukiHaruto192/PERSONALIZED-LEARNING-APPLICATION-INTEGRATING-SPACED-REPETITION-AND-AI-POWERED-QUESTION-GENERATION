package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Option o WHERE o.question.id IN (SELECT q.id FROM Question q WHERE q.quiz.id = :quizId)")
    int deleteAllByQuizId(@Param("quizId") Long quizId);
}
