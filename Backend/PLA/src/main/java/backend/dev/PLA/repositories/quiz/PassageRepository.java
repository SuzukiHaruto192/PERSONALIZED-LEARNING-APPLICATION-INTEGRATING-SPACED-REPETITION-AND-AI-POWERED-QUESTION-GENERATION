package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Passage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PassageRepository extends JpaRepository<Passage, Long> {

    List<Passage> findByQuizIdOrderByPositionAsc(Long quizId);

    Optional<Passage> findByIdAndQuizId(Long id, Long quizId);

    boolean existsByIdAndQuizId(Long id, Long quizId);

    // 0 khi quiz chưa có passage nào
    @Query("SELECT COALESCE(MAX(p.position), 0) FROM Passage p WHERE p.quiz.id = :quizId")
    int findMaxPositionByQuizId(@Param("quizId") Long quizId);

    boolean existsByQuizIdAndPosition(Long quizId, Short position);

    boolean existsByQuizIdAndPositionAndIdNot(Long quizId, Short position, Long id);

    // Flashcard vẫn giữ lại, chỉ bỏ liên kết tới passage sắp bị xoá
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE FlashCard f SET f.sourcePassage = NULL WHERE f.sourcePassage.id = :passageId")
    int detachFlashcardsByPassageId(@Param("passageId") Long passageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE FlashCard f SET f.sourcePassage = NULL "
            + "WHERE f.sourcePassage.id IN (SELECT p.id FROM Passage p WHERE p.quiz.id = :quizId)")
    int detachFlashcardsByQuizId(@Param("quizId") Long quizId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Passage p WHERE p.id = :id")
    int deleteByIdDirectly(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Passage p WHERE p.quiz.id = :quizId")
    int deleteAllByQuizId(@Param("quizId") Long quizId);
}
