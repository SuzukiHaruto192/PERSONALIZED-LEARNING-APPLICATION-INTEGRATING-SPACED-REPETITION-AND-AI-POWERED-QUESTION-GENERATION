package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.FlashCardDetail;
import backend.dev.PLA.entities.FlashCardDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlashCardDetailRepository extends JpaRepository<FlashCardDetail, FlashCardDetailId> {
    boolean existsById(FlashCardDetailId flashCardDetailId);
    Optional<FlashCardDetail> findById(FlashCardDetailId flashCardDetailId);
}
