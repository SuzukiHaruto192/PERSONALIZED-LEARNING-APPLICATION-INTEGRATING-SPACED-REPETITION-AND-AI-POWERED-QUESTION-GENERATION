package backend.dev.PLA.repositories.flashcard;

import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.flashcard.FlashCardDetailId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashCardDetailRepository extends JpaRepository<FlashCardDetail, FlashCardDetailId> {
    boolean existsById(FlashCardDetailId flashCardDetailId);

    Optional<FlashCardDetail> findById(FlashCardDetailId flashCardDetailId);
}
