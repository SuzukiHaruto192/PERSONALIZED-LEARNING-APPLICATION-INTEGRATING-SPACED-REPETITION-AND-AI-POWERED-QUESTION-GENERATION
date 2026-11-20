package backend.dev.PLA.repositories.flashcard;

import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashCardDeckRepository extends JpaRepository<FlashCardDeck, Long> {
    List<FlashCardDeck> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<FlashCardDeck> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndName(Long userId, String name);

    boolean existsByUser_IdAndNameAndIdNot(Long userId, String name, Long id);

    // flashcard_detail của deck tự xoá theo ON DELETE CASCADE
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM FlashCardDeck d WHERE d.id = :id")
    int deleteByIdDirectly(@Param("id") Long id);
}
