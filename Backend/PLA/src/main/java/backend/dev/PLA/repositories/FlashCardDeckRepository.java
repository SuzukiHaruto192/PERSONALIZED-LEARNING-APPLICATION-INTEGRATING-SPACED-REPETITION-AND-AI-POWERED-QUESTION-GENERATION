package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.FlashCardDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashCardDeckRepository extends JpaRepository<FlashCardDeck, Long> {
    List<FlashCardDeck> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<FlashCardDeck> findByIdAndUser_Id(Long id, Long userId);
    boolean existsByUser_IdAndName(Long userId, String name);
}
