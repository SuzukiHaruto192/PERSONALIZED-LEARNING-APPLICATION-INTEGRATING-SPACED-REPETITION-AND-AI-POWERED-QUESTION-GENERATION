package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Option;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {

    List<Option> findByQuestionIdOrderByPositionAsc(Long questionId);

    boolean existsByQuestionId(Long questionId);

    long countByQuestionId(Long questionId);

    // 0 khi câu hỏi chưa có đáp án nào
    @Query("SELECT COALESCE(MAX(o.position), 0) FROM Option o WHERE o.question.id = :questionId")
    int findMaxPositionByQuestionId(@Param("questionId") Long questionId);

    boolean existsByQuestionIdAndPosition(Long questionId, Short position);

    boolean existsByQuestionIdAndPositionAndIdNot(Long questionId, Short position, Long id);

    // Gỡ đáp án đúng hiện tại trước khi đặt đáp án đúng mới (uq_options_one_correct không deferrable)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Option o SET o.isCorrect = false WHERE o.question.id = :questionId AND o.isCorrect = true")
    int unsetCorrectByQuestionId(@Param("questionId") Long questionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Option o SET o.isCorrect = false "
            + "WHERE o.question.id = :questionId AND o.id <> :id AND o.isCorrect = true")
    int unsetCorrectByQuestionIdAndIdNot(@Param("questionId") Long questionId, @Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Option o WHERE o.id = :id")
    int deleteByIdDirectly(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Option o WHERE o.question.id = :questionId")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Option o WHERE o.question.id IN (SELECT q.id FROM Question q WHERE q.quiz.id = :quizId)")
    int deleteAllByQuizId(@Param("quizId") Long quizId);
}
