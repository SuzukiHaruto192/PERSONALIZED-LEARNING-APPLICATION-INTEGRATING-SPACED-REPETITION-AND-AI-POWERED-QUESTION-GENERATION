package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Passage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PassageRepository extends JpaRepository<Passage, Long> {

    // Flashcard vẫn giữ lại, chỉ bỏ liên kết tới passage sắp bị xoá
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE FlashCard f SET f.sourcePassage = NULL "
            + "WHERE f.sourcePassage.id IN (SELECT p.id FROM Passage p WHERE p.quiz.id = :quizId)")
    int detachFlashcardsByQuizId(@Param("quizId") Long quizId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Passage p WHERE p.quiz.id = :quizId")
    int deleteAllByQuizId(@Param("quizId") Long quizId);
}
