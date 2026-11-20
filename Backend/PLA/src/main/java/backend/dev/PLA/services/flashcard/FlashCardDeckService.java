package backend.dev.PLA.services.flashcard;

import backend.dev.PLA.dto.request.flashcard.DeckFlashCardSearchRequest;
import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.DeckStatsResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckDetailResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardDeckMapper;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.repositories.flashcard.DeckStatsView;
import backend.dev.PLA.repositories.flashcard.FlashCardDeckRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardDetailRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardDeckService {
    FlashCardDeckRepository flashCardDeckRepository;
    FlashCardRepository flashCardRepository;
    FlashCardDetailRepository flashCardDetailRepository;
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

    /** Chi tiết deck kèm thống kê thẻ theo trạng thái FSRS. */
    @Transactional(readOnly = true)
    public FlashCardDeckDetailResponse getDeckDetail(CustomUserDetail userDetail, Long deckId) {
        FlashCardDeck deck = getOwnedDeck(userDetail.getId(), deckId);
        DeckStatsView stats = flashCardDetailRepository.countStatsByDeck(deck.getId(), LocalDate.now());
        return FlashCardDeckDetailResponse.builder()
                .id(deck.getId())
                .name(deck.getName())
                .description(deck.getDescription())
                .createdAt(deck.getCreatedAt())
                .stats(DeckStatsResponse.builder()
                        .totalCards(stats.getTotalCards())
                        .dueToday(stats.getDueToday())
                        .newCards(stats.getNewCards())
                        .learning(stats.getLearning())
                        .review(stats.getReview())
                        .relearning(stats.getRelearning())
                        .build())
                .build();
    }

    /** Đổi tên / mô tả deck. Trùng tên với deck khác của tôi → DECK_EXISTED; giữ tên cũ thì cho qua. */
    @Transactional(rollbackFor = Exception.class)
    public FlashCardDeckResponse updateDeck(CustomUserDetail userDetail, Long deckId, NewDeckRequest request) {
        Long userId = userDetail.getId();
        FlashCardDeck deck = getOwnedDeck(userId, deckId);
        String name = request.getName().trim();

        if (flashCardDeckRepository.existsByUser_IdAndNameAndIdNot(userId, name, deckId)) {
            throw new AppException(ErrorCode.DECK_EXISTED);
        }
        deck.setName(name);
        deck.setDescription(request.getDescription());

        try {
            return flashCardDeckMapper.modelToResponse(flashCardDeckRepository.saveAndFlush(deck));
        } catch (DataIntegrityViolationException e) {
            // 2 request đổi tên cùng lúc: UNIQUE (user_id, name) chặn ở DB
            throw new AppException(ErrorCode.DECK_EXISTED);
        }
    }

    /** Xoá deck (flashcard_detail tự xoá theo CASCADE), sau đó dọn các thẻ không còn nằm trong deck nào. */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeck(CustomUserDetail userDetail, Long deckId) {
        FlashCardDeck deck = getOwnedDeck(userDetail.getId(), deckId);
        List<Long> flashCardIds = flashCardDetailRepository.findFlashCardIdsByDeckId(deck.getId());

        flashCardDeckRepository.deleteByIdDirectly(deck.getId());
        if (!flashCardIds.isEmpty()) {
            flashCardRepository.deleteOrphansIn(flashCardIds);
        }
    }

    /** Thẻ trong deck kèm lịch ôn, phân trang, thẻ thêm vào trước đứng trước. */
    @Transactional(readOnly = true)
    public PageResponse<DeckFlashCardResponse> getDeckFlashCards(
            CustomUserDetail userDetail, Long deckId, DeckFlashCardSearchRequest request) {
        FlashCardDeck deck = getOwnedDeck(userDetail.getId(), deckId);
        LocalDate today = LocalDate.now();
        return PageResponse.of(
                flashCardDetailRepository.findPageByDeckId(
                        deck.getId(), PageRequest.of(request.getPage(), request.getSize())),
                detail -> flashCardMapper.detailToResponse(detail, today));
    }

    // Deck của người khác trả về not found để không lộ id deck đang tồn tại
    public FlashCardDeck getOwnedDeck(Long userId, Long deckId) {
        return flashCardDeckRepository
                .findByIdAndUser_Id(deckId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.DECK_NOT_FOUND));
    }
}
