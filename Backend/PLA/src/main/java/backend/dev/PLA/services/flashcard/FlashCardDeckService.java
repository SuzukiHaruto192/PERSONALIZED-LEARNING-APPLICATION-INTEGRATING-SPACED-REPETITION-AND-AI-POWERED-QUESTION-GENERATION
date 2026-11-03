package backend.dev.PLA.services.flashcard;

import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.FlashCardDeck;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardDeckMapper;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.repositories.FlashCardDeckRepository;
import backend.dev.PLA.repositories.FlashCardRepository;
import backend.dev.PLA.repositories.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardDeckService {
    FlashCardDeckRepository flashCardDeckRepository;
    FlashCardRepository flashCardRepository;
    UserRepository userRepository;
    FlashCardDeckMapper flashCardDeckMapper;
    FlashCardMapper flashCardMapper;

    public List<FlashCardDeckResponse> getMyDecks(CustomUserDetail userDetail) {
        return flashCardDeckMapper.modelsToResponses(
                flashCardDeckRepository.findAllByUser_IdOrderByCreatedAtDesc(userDetail.getId()));
    }

    @Transactional
    public FlashCardDeckResponse createDeck(CustomUserDetail userDetail, NewDeckRequest request) {
        Long userId = userDetail.getId();
        String name = request.getName().trim();

        if (flashCardDeckRepository.existsByUser_IdAndName(userId, name)) {
            throw new AppException(ErrorCode.DECK_EXISTED);
        }

        User user = userRepository.getReferenceById(userId);
        FlashCardDeck deck = FlashCardDeck.builder()
                .user(user)
                .name(name)
                .description(request.getDescription())
                .build();

        return flashCardDeckMapper.modelToResponse(flashCardDeckRepository.save(deck));
    }

    public List<FlashCardResponse> getDeckFlashCards(CustomUserDetail userDetail, Long deckId) {
        FlashCardDeck deck = getOwnedDeck(userDetail.getId(), deckId);
        return flashCardMapper.modelsToResponses(flashCardRepository.findAllByDeckId(deck.getId()));
    }

    // Deck của người khác trả về not found để không lộ id deck đang tồn tại
    public FlashCardDeck getOwnedDeck(Long userId, Long deckId) {
        return flashCardDeckRepository.findByIdAndUser_Id(deckId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.DECK_NOT_FOUND));
    }
}
