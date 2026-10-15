package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashCardRepository extends JpaRepository<FlashCard,Long> {
    @Query(
            value = """
            SELECT DISTINCT fc.* 
            FROM flashcard fc
            INNER JOIN flashcard_detail fcd ON fc.id = fcd.id_flashcard
            INNER JOIN users u ON fcd.id_user = u.id
            WHERE u.username = :username
       """,
            nativeQuery = true
    )
    List<FlashCard> findAllByUsername(@Param("username") String username);
}
