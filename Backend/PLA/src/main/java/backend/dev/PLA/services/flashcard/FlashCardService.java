package backend.dev.PLA.services.flashcard;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.dto.request.flashcard.AddFlashCardsRequest;
import backend.dev.PLA.dto.request.flashcard.DueFlashCardSearchRequest;
import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.UpdateFlashCardRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.flashcard.AddFlashCardsResponse;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.flashcard.FlashCardDetailId;
import backend.dev.PLA.entities.quiz.Passage;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.repositories.flashcard.FlashCardDetailRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.quiz.PassageRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.QuizAccessService;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
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
    PassageRepository passageRepository;
    QuizAccessService quizAccessService;

    public List<FlashCardResponse> getMyFlashCard(CustomUserDetail userDetail) {
        List<FlashCard> flashCards = flashCardRepository.findAllByUsername(userDetail.getUsername());
        return flashCardMapper.modelsToResponses(flashCards);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createFlashCard(CustomUserDetail userDetail, NewFlashCardRequest request) {
        addNewCardToDeck(userDetail.getId(), request, null);
    }

    /** Tạo thẻ từ từ lạ trong passage: người học phải xem được quiz chứa passage. */
    @Transactional(rollbackFor = Exception.class)
    public DeckFlashCardResponse createFlashCardFromPassage(
            CustomUserDetail userDetail, Long passageId, NewFlashCardRequest request) {
        Passage passage =
                passageRepository.findById(passageId).orElseThrow(() -> new AppException(ErrorCode.PASSAGE_NOT_FOUND));
        quizAccessService.findViewableQuiz(userDetail.getId(), passage.getQuiz().getId());

        FlashCardDetail detail = addNewCardToDeck(userDetail.getId(), request, passage);
        return flashCardMapper.detailToResponse(detail, LocalDate.now());
    }

    /** Thẻ đến hạn (next_review <= hôm nay) có phân trang, quá hạn lâu nhất đứng trước. Không gồm thẻ mới. */
    @Transactional(readOnly = true)
    public PageResponse<DeckFlashCardResponse> getDueFlashCards(
            CustomUserDetail userDetail, DueFlashCardSearchRequest request) {
        Long userId = userDetail.getId();
        Long deckId = request.getDeckId();
        if (deckId != null) {
            flashCardDeckService.getOwnedDeck(userId, deckId);
        }
        LocalDate today = LocalDate.now();

        return PageResponse.of(
                flashCardDetailRepository.findDue(
                        userId, deckId, today, PageRequest.of(request.getPage(), request.getSize())),
                detail -> flashCardMapper.detailToResponse(detail, today));
    }

    @Transactional(rollbackFor = Exception.class)
    public DeckFlashCardResponse reviewFlashCard(CustomUserDetail userDetail, FlashCardRequest request) {
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

        detail = flashCardDetailRepository.save(detail);
        return flashCardMapper.detailToResponse(detail, LocalDate.now());
    }

    /**
     * Sửa nội dung thẻ, không đổi lịch ôn. Thẻ còn nằm trong deck của user khác (dữ liệu cũ dùng chung) thì
     * copy-on-write: tạo thẻ mới cho user hiện tại và chuyển các dòng flashcard_detail của user sang thẻ mới.
     */
    @Transactional(rollbackFor = Exception.class)
    public FlashCardResponse updateFlashCard(
            CustomUserDetail userDetail, Long flashCardId, UpdateFlashCardRequest request) {
        Long userId = userDetail.getId();
        List<FlashCardDetail> myDetails = flashCardDetailRepository.findAllByFlashCardIdAndUserId(flashCardId, userId);
        if (myDetails.isEmpty()) {
            throw new AppException(ErrorCode.FLASHCARD_NOT_FOUND);
        }

        String front = request.getFrontContent().trim();
        String back = request.getBackContent().trim();
        String example = trimToNull(request.getExample());
        if (flashCardRepository.existsOtherInUserDecksByContent(userId, front, back, flashCardId)) {
            throw new AppException(ErrorCode.FLASHCARD_CONTENT_EXISTED);
        }

        FlashCard card = myDetails.getFirst().getFlashCard();
        if (flashCardDetailRepository.existsByFlashCardIdAndDeckUserIdNot(flashCardId, userId)) {
            FlashCard copy = flashCardRepository.saveAndFlush(FlashCard.builder()
                    .createdBy(userRepository.getReferenceById(userId))
                    .frontContent(front)
                    .backContent(back)
                    .example(example)
                    .sourcePassage(card.getSourcePassage())
                    .build());
            flashCardDetailRepository.moveToFlashCard(flashCardId, copy.getId(), userId);
            return flashCardMapper.modelToResponse(copy);
        }

        card.setFrontContent(front);
        card.setBackContent(back);
        card.setExample(example);
        return flashCardMapper.modelToResponse(flashCardRepository.save(card));
    }

    /** Gỡ thẻ khỏi mọi deck của tôi; chỉ xoá dòng flashcard khi không còn deck nào (của bất kỳ ai) tham chiếu. */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFlashCard(CustomUserDetail userDetail, Long flashCardId) {
        int deleted = flashCardDetailRepository.deleteByFlashCardIdAndUserId(flashCardId, userDetail.getId());
        if (deleted == 0) {
            throw new AppException(ErrorCode.FLASHCARD_NOT_FOUND);
        }
        flashCardRepository.deleteOrphansIn(List.of(flashCardId));
    }

    /** Gỡ thẻ khỏi 1 deck (mất lịch ôn của thẻ trong deck này). */
    @Transactional(rollbackFor = Exception.class)
    public void removeFromDeck(CustomUserDetail userDetail, Long deckId, Long flashCardId) {
        FlashCardDeck deck = flashCardDeckService.getOwnedDeck(userDetail.getId(), deckId);
        int deleted = flashCardDetailRepository.deleteByDeckIdAndFlashCardId(deck.getId(), flashCardId);
        if (deleted == 0) {
            throw new AppException(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND);
        }
        flashCardRepository.deleteOrphansIn(List.of(flashCardId));
    }

    /**
     * Thêm các thẻ có sẵn (đang nằm trong một deck của tôi) vào deck khác. Một id không hợp lệ thì không thêm thẻ
     * nào; thẻ đã có trong deck thì bỏ qua. Thẻ được thêm bắt đầu lịch ôn mới (NEW).
     */
    @Transactional(rollbackFor = Exception.class)
    public AddFlashCardsResponse addExistingFlashCards(
            CustomUserDetail userDetail, Long deckId, AddFlashCardsRequest request) {
        Long userId = userDetail.getId();
        FlashCardDeck deck = flashCardDeckService.getOwnedDeck(userId, deckId);

        Set<Long> ids = new LinkedHashSet<>(request.getFlashCardIds());
        if (flashCardDetailRepository.findOwnedFlashCardIds(userId, ids).size() != ids.size()) {
            throw new AppException(ErrorCode.FLASHCARD_NOT_FOUND);
        }
        Set<Long> existing = new HashSet<>(flashCardDetailRepository.findFlashCardIdsInDeck(deck.getId(), ids));

        List<FlashCardDetail> newDetails = ids.stream()
                .filter(id -> !existing.contains(id))
                .map(id -> FlashCardDetail.builder()
                        .id(new FlashCardDetailId(deck.getId(), id))
                        .deck(deck)
                        .flashCard(flashCardRepository.getReferenceById(id))
                        .build())
                .toList();
        flashCardDetailRepository.saveAll(newDetails);

        return AddFlashCardsResponse.builder()
                .added(newDetails.size())
                .skipped(ids.size() - newDetails.size())
                .build();
    }

    /** Học lại thẻ từ đầu: đưa lịch ôn của cặp (deck, thẻ) về trạng thái NEW. */
    @Transactional(rollbackFor = Exception.class)
    public DeckFlashCardResponse resetFlashCard(CustomUserDetail userDetail, Long deckId, Long flashCardId) {
        FlashCardDeck deck = flashCardDeckService.getOwnedDeck(userDetail.getId(), deckId);
        FlashCardDetail detail = flashCardDetailRepository
                .findById(new FlashCardDetailId(deck.getId(), flashCardId))
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND));

        detail.setFsrsState(FsrsState.NEW);
        detail.setStability(null);
        detail.setDifficulty(null);
        detail.setLastReviewDate(null);
        detail.setNextReviewDate(null);
        detail.setLapses(0);

        detail = flashCardDetailRepository.save(detail);
        return flashCardMapper.detailToResponse(detail, LocalDate.now());
    }

    /**
     * Đưa thẻ vào deck: tái sử dụng thẻ cùng nội dung do chính user tạo (không dùng chung thẻ của người khác), nếu
     * không có thì tạo thẻ mới. Thẻ mới trong deck để stability/difficulty = NULL (chưa ôn lần nào).
     */
    private FlashCardDetail addNewCardToDeck(Long userId, NewFlashCardRequest request, Passage passage) {
        FlashCardDeck deck = flashCardDeckService.getOwnedDeck(userId, request.getDeckId());

        String front = request.getFrontContent().trim();
        String back = request.getBackContent().trim();

        FlashCard flashCard =
                flashCardRepository.findByOwnerAndContent(userId, front, back).orElse(null);
        if (flashCard == null) {
            flashCard = flashCardRepository.save(FlashCard.builder()
                    .createdBy(userRepository.getReferenceById(userId))
                    .frontContent(front)
                    .backContent(back)
                    .example(trimToNull(request.getExample()))
                    .sourcePassage(passage)
                    .build());
        } else {
            if (flashCardDetailRepository.existsById(new FlashCardDetailId(deck.getId(), flashCard.getId()))) {
                throw new AppException(ErrorCode.FLASHCARD_DETAIL_EXISTED);
            }
            if (passage != null && flashCard.getSourcePassage() == null) {
                flashCard.setSourcePassage(passage);
            }
        }

        FlashCardDetail detail = FlashCardDetail.builder()
                .id(new FlashCardDetailId(deck.getId(), flashCard.getId()))
                .deck(deck)
                .flashCard(flashCard)
                .build();
        return flashCardDetailRepository.save(detail);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
