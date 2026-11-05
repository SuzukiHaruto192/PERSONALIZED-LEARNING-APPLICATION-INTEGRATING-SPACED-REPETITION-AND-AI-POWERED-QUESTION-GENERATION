package backend.dev.PLA.services.flashcard;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.flashcard.FlashCardDetailId;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.repositories.flashcard.FlashCardDetailRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardService {
    FlashCardRepository flashCardRepository;
    FlashCardMapper flashCardMapper;
    UserRepository userRepository;
    FlashCardDetailRepository flashCardDetailRepository;
    SpacedRepetitionService spacedRepetitionService;
    FlashCardDeckService flashCardDeckService;

    public List<FlashCardResponse> getMyFlashCard(CustomUserDetail userDetail) {
        List<FlashCard> flashCards = flashCardRepository.findAllByUsername(userDetail.getUsername());
        return flashCardMapper.modelsToResponses(flashCards);
    }

    @Transactional
    public void createFlashCard(CustomUserDetail userDetail, NewFlashCardRequest newflashCardRequest) {
        FlashCardDeck deck = flashCardDeckService.getOwnedDeck(userDetail.getId(), newflashCardRequest.getDeckId());
        User user = userRepository.getReferenceById(userDetail.getId());

        String front = newflashCardRequest.getFrontContent().trim();
        String back = newflashCardRequest.getBackContent().trim();

        FlashCard flashCard = flashCardRepository.findByContent(front, back);

        if (flashCard == null) {
            flashCard = flashCardMapper.requestToModel(newflashCardRequest);
            flashCard.setFrontContent(front);
            flashCard.setBackContent(back);
            flashCard.setCreatedBy(user);
            flashCard = flashCardRepository.save(flashCard);
        } else {
            FlashCardDetailId checkId = new FlashCardDetailId(deck.getId(), flashCard.getId());
            if (flashCardDetailRepository.existsById(checkId)) {
                throw new AppException(ErrorCode.FLASHCARD_DETAIL_EXISTED);
            }
        }

        FlashCardDetail detail = FlashCardDetail.builder()
                .id(new FlashCardDetailId(deck.getId(), flashCard.getId()))
                .deck(deck)
                .flashCard(flashCard)
                .stability(0.0)
                .difficulty(0.0)
                .build();

        flashCardDetailRepository.save(detail);
    }

    @Transactional
    public FSRSResult reviewFlashCard(CustomUserDetail userDetail, FlashCardRequest request) {
        FlashCardDeck deck = flashCardDeckService.getOwnedDeck(userDetail.getId(), request.getDeckId());

        FlashCardDetailId detailId = new FlashCardDetailId(deck.getId(), request.getFlashCardId());
        FlashCardDetail detail = flashCardDetailRepository
                .findById(detailId)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_NOT_FOUND));

        FSRSResult result = spacedRepetitionService.calculateNextReview(
                detail.getStability(), detail.getDifficulty(), detail.getLastReviewDate(), request.getRating());

        detail.setStability(result.getStability());
        detail.setDifficulty(result.getDifficulty());
        detail.setNextReviewDate(result.getNextReviewAt().toLocalDate());
        detail.setLastReviewDate(result.getLastReviewAt().toLocalDate());
        detail.setLapses(request.getRating() == 1 ? detail.getLapses() + 1 : detail.getLapses());
        detail.setFsrsState(spacedRepetitionService.nextState(detail.getFsrsState(), request.getRating()));

        flashCardDetailRepository.save(detail);

        return result;
    }
}
