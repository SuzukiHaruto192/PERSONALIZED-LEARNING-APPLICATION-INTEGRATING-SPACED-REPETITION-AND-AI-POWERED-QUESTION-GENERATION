package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.mapper.FlashCardMapperImpl;
import backend.dev.PLA.repositories.flashcard.FlashCardDetailRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.quiz.PassageRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import backend.dev.PLA.services.flashcard.FlashCardService;
import backend.dev.PLA.services.quiz.QuizAccessService;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class FlashCardServiceTest {
    @Mock
    private FlashCardRepository flashCardRepository;

    @InjectMocks
    private FlashCardService flashCardService;

    @Spy
    FlashCardMapper flashCardMapper = new FlashCardMapperImpl();

    @Mock
    UserRepository userRepository;

    @Mock
    FlashCardDetailRepository flashCardDetailRepository;

    @Spy
    SpacedRepetitionService spacedRepetitionService;

    @Mock
    FlashCardDeckService flashCardDeckService;

    @Mock
    PassageRepository passageRepository;

    @Mock
    QuizAccessService quizAccessService;

    private FlashCardRequest flashCardRequest;

    private NewFlashCardRequest newFlashCardRequest;

    private User user;

    private CustomUserDetail customUserDetail;

    private FlashCard flashCard;

    private FlashCardDeck deck;

    private FlashCardDetail flashCardDetail;

    @BeforeEach
    void initData() {
        newFlashCardRequest = NewFlashCardRequest.builder()
                .deckId(1L)
                .frontContent("apple")
                .backContent("táo")
                .build();
        flashCardRequest =
                FlashCardRequest.builder().deckId(1L).flashCardId(1L).rating(3).build();
        user = User.builder()
                .id(1L)
                .username("test")
                .email("test@gmail.com")
                .password("123456")
                .build();
        customUserDetail = new CustomUserDetail(user);

        deck = FlashCardDeck.builder().id(1L).user(user).name("deck").build();
        lenient().when(flashCardDeckService.getOwnedDeck(1L, 1L)).thenReturn(deck);

        flashCard = FlashCard.builder()
                .id(1L)
                .frontContent("apple")
                .backContent("táo")
                .build();

        flashCardDetail = FlashCardDetail.builder()
                .id(new FlashCardDetailId(deck.getId(), flashCard.getId()))
                .lapses(0)
                .lastReviewDate(null)
                .nextReviewDate(null)
                .deck(deck)
                .flashCard(flashCard)
                .build();
        lenient()
                .when(flashCardDetailRepository.save(any(FlashCardDetail.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    public void getMyFlashCardsTest() {
        when(flashCardRepository.findAllByUsername("test")).thenReturn(List.of(flashCard));

        List<FlashCardResponse> response = flashCardService.getMyFlashCard(customUserDetail);

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals("apple", response.get(0).getFrontContent());
        assertEquals("táo", response.get(0).getBackContent());

        verify(flashCardRepository).findAllByUsername("test");
    }

    @Test
    public void getMyFlashCardsTest_empty() {
        when(flashCardRepository.findAllByUsername("test")).thenReturn(List.of());

        List<FlashCardResponse> response = flashCardService.getMyFlashCard(customUserDetail);

        assertTrue(response.isEmpty());
    }

    // ===================== create =====================

    @Test
    public void createFlashCardTest_newCard() {
        NewFlashCardRequest request = NewFlashCardRequest.builder()
                .deckId(1L)
                .frontContent("  apple  ")
                .backContent("  táo  ")
                .example("   ")
                .build();
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardRepository.findByOwnerAndContent(1L, "apple", "táo")).thenReturn(Optional.empty());
        when(flashCardRepository.save(any(FlashCard.class))).thenReturn(flashCard);

        flashCardService.createFlashCard(customUserDetail, request);

        ArgumentCaptor<FlashCard> cardCaptor = ArgumentCaptor.forClass(FlashCard.class);
        verify(flashCardRepository).save(cardCaptor.capture());
        assertEquals("apple", cardCaptor.getValue().getFrontContent());
        assertEquals("táo", cardCaptor.getValue().getBackContent());
        assertNull(cardCaptor.getValue().getExample());
        assertSame(user, cardCaptor.getValue().getCreatedBy());

        assertInitialDetail(captureSavedDetail());
        verify(flashCardDetailRepository, never()).existsById(any());
    }

    @Test
    public void createFlashCardTest_existingCardNotInUserCollection() {
        when(flashCardRepository.findByOwnerAndContent(1L, "apple", "táo")).thenReturn(Optional.of(flashCard));
        when(flashCardDetailRepository.existsById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(false);

        flashCardService.createFlashCard(customUserDetail, newFlashCardRequest);

        verify(flashCardRepository, never()).save(any());
        assertInitialDetail(captureSavedDetail());
    }

    @Test
    public void createFlashCardTest_existingCardAlreadyInUserCollection() {
        when(flashCardRepository.findByOwnerAndContent(1L, "apple", "táo")).thenReturn(Optional.of(flashCard));
        when(flashCardDetailRepository.existsById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> flashCardService.createFlashCard(customUserDetail, newFlashCardRequest));

        assertEquals(ErrorCode.FLASHCARD_DETAIL_EXISTED, exception.getErrorCode());
        verify(flashCardRepository, never()).save(any());
        verify(flashCardDetailRepository, never()).save(any());
    }

    @Test
    public void createFlashCardTest_deckOfAnotherUser() {
        when(flashCardDeckService.getOwnedDeck(1L, 2L)).thenThrow(new AppException(ErrorCode.DECK_NOT_FOUND));
        newFlashCardRequest.setDeckId(2L);

        AppException exception = assertThrows(
                AppException.class, () -> flashCardService.createFlashCard(customUserDetail, newFlashCardRequest));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
        verify(flashCardRepository, never()).save(any());
        verify(flashCardDetailRepository, never()).save(any());
    }

    // ===================== review =====================

    @Test
    public void reviewFlashCardTest_deckOfAnotherUser() {
        when(flashCardDeckService.getOwnedDeck(1L, 2L)).thenThrow(new AppException(ErrorCode.DECK_NOT_FOUND));
        flashCardRequest.setDeckId(2L);

        AppException exception = assertThrows(
                AppException.class, () -> flashCardService.reviewFlashCard(customUserDetail, flashCardRequest));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
        verify(flashCardDetailRepository, never()).findById(any());
        verify(flashCardDetailRepository, never()).save(any());
    }

    @Test
    public void reviewFlashCardTest_detailNotFound() {
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L))).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class, () -> flashCardService.reviewFlashCard(customUserDetail, flashCardRequest));

        assertEquals(ErrorCode.FLASHCARD_NOT_FOUND, exception.getErrorCode());
        verify(spacedRepetitionService, never()).calculateNextReview(any(), any(), any(LocalDate.class), anyInt());
        verify(flashCardDetailRepository, never()).save(any());
    }

    @Test
    public void reviewFlashCardTest_firstReview() {
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(Optional.of(flashCardDetail));

        DeckFlashCardResponse response = flashCardService.reviewFlashCard(customUserDetail, flashCardRequest);

        // Giá trị khởi tạo của FSRS với rating 3: S0 = w[2], D0 = w[4], interval 4 ngày
        FlashCardDetail saved = captureSavedDetail();
        assertEquals(3.7145, saved.getStability());
        assertEquals(5.1618, saved.getDifficulty());
        assertEquals(LocalDate.now().plusDays(4), saved.getNextReviewDate());
        assertEquals(FsrsState.REVIEW, response.getFsrsState());
        assertEquals(saved.getNextReviewDate(), response.getNextReviewDate());
        assertEquals(1L, response.getDeckId());
        assertEquals(1L, response.getFlashCardId());
    }

    @Test
    public void reviewFlashCardTest_repeatReviewUsesStoredState() {
        LocalDate lastReview = LocalDate.of(2026, 1, 1);
        flashCardDetail.setStability(5.0);
        flashCardDetail.setDifficulty(6.0);
        flashCardDetail.setLastReviewDate(lastReview);
        flashCardDetail.setLapses(2);
        flashCardDetail.setFsrsState(FsrsState.REVIEW);
        FSRSResult expected = FSRSResult.builder()
                .stability(12.5)
                .difficulty(5.5)
                .intervalDays(13)
                .lastReviewAt(LocalDateTime.of(2026, 1, 4, 8, 0))
                .nextReviewAt(LocalDateTime.of(2026, 1, 17, 8, 0))
                .build();
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(Optional.of(flashCardDetail));
        doReturn(expected).when(spacedRepetitionService).calculateNextReview(5.0, 6.0, lastReview, 3);

        DeckFlashCardResponse response = flashCardService.reviewFlashCard(customUserDetail, flashCardRequest);

        assertDetailMatchesResult(captureSavedDetail(), expected, 2, FsrsState.REVIEW);
        assertEquals(LocalDate.of(2026, 1, 17), response.getNextReviewDate());
        assertEquals(2, response.getLapses());
    }

    @Test
    public void reviewFlashCardTest_ratingAgainIncreasesLapses() {
        flashCardDetail.setLapses(2);
        flashCardDetail.setFsrsState(FsrsState.REVIEW);
        FlashCardRequest request =
                FlashCardRequest.builder().deckId(1L).flashCardId(1L).rating(1).build();
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(Optional.of(flashCardDetail));

        DeckFlashCardResponse response = flashCardService.reviewFlashCard(customUserDetail, request);

        FlashCardDetail saved = captureSavedDetail();
        assertEquals(3, saved.getLapses());
        assertEquals(FsrsState.RELEARNING, saved.getFsrsState());
        assertEquals(FsrsState.RELEARNING, response.getFsrsState());
    }

    // ===================== due =====================

    @Test
    public void getDueFlashCards_pagedDueCards() {
        FlashCardDetail due =
                detailWithCard(2L, FsrsState.REVIEW, LocalDate.now().minusDays(3));
        when(flashCardDetailRepository.findDue(eq(1L), isNull(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(due), PageRequest.of(1, 5), 6));

        PageResponse<DeckFlashCardResponse> response = flashCardService.getDueFlashCards(
                customUserDetail,
                DueFlashCardSearchRequest.builder().page(1).size(5).build());

        assertEquals(6L, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertEquals(1, response.getPage());
        assertEquals(
                List.of(2L),
                response.getContent().stream()
                        .map(DeckFlashCardResponse::getFlashCardId)
                        .toList());
        assertEquals(3, response.getContent().getFirst().getOverdueDays());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(flashCardDetailRepository).findDue(eq(1L), isNull(), any(LocalDate.class), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        verify(flashCardDeckService, never()).getOwnedDeck(anyLong(), anyLong());
    }

    @Test
    public void getDueFlashCards_filterByOwnedDeck() {
        when(flashCardDetailRepository.findDue(eq(1L), eq(1L), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(detailWithCard(2L, FsrsState.REVIEW, LocalDate.now()))));

        PageResponse<DeckFlashCardResponse> response = flashCardService.getDueFlashCards(
                customUserDetail, DueFlashCardSearchRequest.builder().deckId(1L).build());

        assertEquals(1, response.getContent().size());
        verify(flashCardDeckService).getOwnedDeck(1L, 1L);
    }

    @Test
    public void getDueFlashCards_deckOfAnotherUser() {
        when(flashCardDeckService.getOwnedDeck(1L, 9L)).thenThrow(new AppException(ErrorCode.DECK_NOT_FOUND));

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardService.getDueFlashCards(
                        customUserDetail,
                        DueFlashCardSearchRequest.builder().deckId(9L).build()));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
        verify(flashCardDetailRepository, never()).findDue(any(), any(), any(), any());
    }

    // ===================== update =====================

    @Test
    public void updateFlashCard_notInMyDecks() {
        when(flashCardDetailRepository.findAllByFlashCardIdAndUserId(1L, 1L)).thenReturn(List.of());

        AppException exception = assertThrows(
                AppException.class, () -> flashCardService.updateFlashCard(customUserDetail, 1L, updateRequest()));

        assertEquals(ErrorCode.FLASHCARD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    public void updateFlashCard_contentExisted() {
        when(flashCardDetailRepository.findAllByFlashCardIdAndUserId(1L, 1L)).thenReturn(List.of(flashCardDetail));
        when(flashCardRepository.existsOtherInUserDecksByContent(1L, "pear", "lê", 1L))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> flashCardService.updateFlashCard(customUserDetail, 1L, updateRequest()));

        assertEquals(ErrorCode.FLASHCARD_CONTENT_EXISTED, exception.getErrorCode());
        verify(flashCardRepository, never()).save(any());
        verify(flashCardRepository, never()).saveAndFlush(any());
    }

    @Test
    public void updateFlashCard_notShared_editsInPlace() {
        when(flashCardDetailRepository.findAllByFlashCardIdAndUserId(1L, 1L)).thenReturn(List.of(flashCardDetail));
        when(flashCardDetailRepository.existsByFlashCardIdAndDeckUserIdNot(1L, 1L))
                .thenReturn(false);
        when(flashCardRepository.save(flashCard)).thenReturn(flashCard);

        FlashCardResponse response = flashCardService.updateFlashCard(customUserDetail, 1L, updateRequest());

        assertEquals(1L, response.getId());
        assertEquals("pear", flashCard.getFrontContent());
        assertEquals("lê", flashCard.getBackContent());
        assertEquals("A ripe pear.", flashCard.getExample());
        verify(flashCardDetailRepository, never()).moveToFlashCard(any(), any(), any());
    }

    @Test
    public void updateFlashCard_shared_copyOnWrite() {
        when(flashCardDetailRepository.findAllByFlashCardIdAndUserId(1L, 1L)).thenReturn(List.of(flashCardDetail));
        when(flashCardDetailRepository.existsByFlashCardIdAndDeckUserIdNot(1L, 1L))
                .thenReturn(true);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardRepository.saveAndFlush(any(FlashCard.class))).thenAnswer(inv -> {
            FlashCard card = inv.getArgument(0);
            card.setId(99L);
            return card;
        });

        FlashCardResponse response = flashCardService.updateFlashCard(customUserDetail, 1L, updateRequest());

        assertEquals(99L, response.getId());
        assertEquals("pear", response.getFrontContent());
        // thẻ gốc (user khác vẫn dùng) giữ nguyên nội dung
        assertEquals("apple", flashCard.getFrontContent());
        verify(flashCardDetailRepository).moveToFlashCard(1L, 99L, 1L);
        verify(flashCardRepository, never()).save(any());
    }

    // ===================== delete / remove =====================

    @Test
    public void deleteFlashCard_success_cleansOrphan() {
        when(flashCardDetailRepository.deleteByFlashCardIdAndUserId(1L, 1L)).thenReturn(2);

        flashCardService.deleteFlashCard(customUserDetail, 1L);

        verify(flashCardRepository).deleteOrphansIn(List.of(1L));
    }

    @Test
    public void deleteFlashCard_notFound() {
        when(flashCardDetailRepository.deleteByFlashCardIdAndUserId(1L, 1L)).thenReturn(0);

        AppException exception =
                assertThrows(AppException.class, () -> flashCardService.deleteFlashCard(customUserDetail, 1L));

        assertEquals(ErrorCode.FLASHCARD_NOT_FOUND, exception.getErrorCode());
        verify(flashCardRepository, never()).deleteOrphansIn(any());
    }

    @Test
    public void removeFromDeck_success() {
        when(flashCardDetailRepository.deleteByDeckIdAndFlashCardId(1L, 1L)).thenReturn(1);

        flashCardService.removeFromDeck(customUserDetail, 1L, 1L);

        verify(flashCardRepository).deleteOrphansIn(List.of(1L));
    }

    @Test
    public void removeFromDeck_detailNotFound() {
        when(flashCardDetailRepository.deleteByDeckIdAndFlashCardId(1L, 1L)).thenReturn(0);

        AppException exception =
                assertThrows(AppException.class, () -> flashCardService.removeFromDeck(customUserDetail, 1L, 1L));

        assertEquals(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND, exception.getErrorCode());
    }

    // ===================== add existing =====================

    @Test
    @SuppressWarnings("unchecked")
    public void addExistingFlashCards_countsAddedAndSkipped() {
        when(flashCardDetailRepository.findOwnedFlashCardIds(1L, Set.of(41L, 42L)))
                .thenReturn(List.of(41L, 42L));
        when(flashCardDetailRepository.findFlashCardIdsInDeck(1L, Set.of(41L, 42L)))
                .thenReturn(List.of(42L));
        when(flashCardRepository.getReferenceById(41L))
                .thenReturn(FlashCard.builder().id(41L).build());

        AddFlashCardsResponse response = flashCardService.addExistingFlashCards(
                customUserDetail,
                1L,
                AddFlashCardsRequest.builder()
                        .flashCardIds(List.of(41L, 42L, 42L))
                        .build());

        assertEquals(1, response.getAdded());
        assertEquals(1, response.getSkipped());
        ArgumentCaptor<List<FlashCardDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(flashCardDetailRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(
                new FlashCardDetailId(1L, 41L), captor.getValue().getFirst().getId());
        assertEquals(FsrsState.NEW, captor.getValue().getFirst().getFsrsState());
    }

    @Test
    public void addExistingFlashCards_oneInvalidId_addsNothing() {
        when(flashCardDetailRepository.findOwnedFlashCardIds(1L, Set.of(41L, 777L)))
                .thenReturn(List.of(41L));

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardService.addExistingFlashCards(
                        customUserDetail,
                        1L,
                        AddFlashCardsRequest.builder()
                                .flashCardIds(List.of(41L, 777L))
                                .build()));

        assertEquals(ErrorCode.FLASHCARD_NOT_FOUND, exception.getErrorCode());
        verify(flashCardDetailRepository, never()).saveAll(any());
    }

    // ===================== from passage =====================

    @Test
    public void createFlashCardFromPassage_setsSourcePassage() {
        Passage passage = passageOfQuiz(12L, 7L);
        when(passageRepository.findById(12L)).thenReturn(Optional.of(passage));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardRepository.findByOwnerAndContent(1L, "apple", "táo")).thenReturn(Optional.empty());
        when(flashCardRepository.save(any(FlashCard.class))).thenAnswer(inv -> {
            FlashCard card = inv.getArgument(0);
            card.setId(5L);
            return card;
        });

        DeckFlashCardResponse response =
                flashCardService.createFlashCardFromPassage(customUserDetail, 12L, newFlashCardRequest);

        verify(quizAccessService).findViewableQuiz(1L, 7L);
        assertEquals(5L, response.getFlashCardId());
        assertEquals(12L, response.getSourcePassageId());
        assertEquals(FsrsState.NEW, response.getFsrsState());
    }

    @Test
    public void createFlashCardFromPassage_passageNotFound() {
        when(passageRepository.findById(12L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardService.createFlashCardFromPassage(customUserDetail, 12L, newFlashCardRequest));

        assertEquals(ErrorCode.PASSAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    public void createFlashCardFromPassage_quizOfAnotherUser() {
        when(passageRepository.findById(12L)).thenReturn(Optional.of(passageOfQuiz(12L, 7L)));
        when(quizAccessService.findViewableQuiz(1L, 7L)).thenThrow(new AppException(ErrorCode.ACCESS_DENIED));

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardService.createFlashCardFromPassage(customUserDetail, 12L, newFlashCardRequest));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(flashCardRepository, never()).save(any());
        verify(flashCardDetailRepository, never()).save(any());
    }

    // ===================== reset =====================

    @Test
    public void resetFlashCard_backToNew() {
        flashCardDetail.setStability(5.0);
        flashCardDetail.setDifficulty(6.0);
        flashCardDetail.setLapses(3);
        flashCardDetail.setFsrsState(FsrsState.RELEARNING);
        flashCardDetail.setLastReviewDate(LocalDate.of(2026, 1, 1));
        flashCardDetail.setNextReviewDate(LocalDate.of(2026, 1, 2));
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(Optional.of(flashCardDetail));

        DeckFlashCardResponse response = flashCardService.resetFlashCard(customUserDetail, 1L, 1L);

        assertInitialDetail(captureSavedDetail());
        assertEquals(FsrsState.NEW, response.getFsrsState());
        assertEquals(0, response.getOverdueDays());
    }

    @Test
    public void resetFlashCard_detailNotFound() {
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L))).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> flashCardService.resetFlashCard(customUserDetail, 1L, 1L));

        assertEquals(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND, exception.getErrorCode());
    }

    // ===================== helpers =====================

    private UpdateFlashCardRequest updateRequest() {
        return UpdateFlashCardRequest.builder()
                .frontContent(" pear ")
                .backContent("lê")
                .example("A ripe pear.")
                .build();
    }

    private Passage passageOfQuiz(Long passageId, Long quizId) {
        return Passage.builder()
                .id(passageId)
                .quiz(Quiz.builder().id(quizId).build())
                .build();
    }

    private FlashCardDetail detailWithCard(Long cardId, FsrsState state, LocalDate nextReview) {
        FlashCard card = FlashCard.builder()
                .id(cardId)
                .frontContent("front " + cardId)
                .backContent("back " + cardId)
                .build();
        return FlashCardDetail.builder()
                .id(new FlashCardDetailId(deck.getId(), cardId))
                .deck(deck)
                .flashCard(card)
                .fsrsState(state)
                .nextReviewDate(nextReview)
                .build();
    }

    private FlashCardDetail captureSavedDetail() {
        ArgumentCaptor<FlashCardDetail> captor = ArgumentCaptor.forClass(FlashCardDetail.class);
        verify(flashCardDetailRepository).save(captor.capture());
        return captor.getValue();
    }

    private void assertInitialDetail(FlashCardDetail detail) {
        assertEquals(new FlashCardDetailId(1L, 1L), detail.getId());
        assertSame(flashCard, detail.getFlashCard());
        assertSame(deck, detail.getDeck());
        // Thẻ mới: NULL = chưa ôn lần nào
        assertNull(detail.getStability());
        assertNull(detail.getDifficulty());
        assertNull(detail.getNextReviewDate());
        assertNull(detail.getLastReviewDate());
        assertEquals(0, detail.getLapses());
        assertEquals(FsrsState.NEW, detail.getFsrsState());
    }

    private void assertDetailMatchesResult(
            FlashCardDetail detail, FSRSResult result, int expectedLapses, FsrsState expectedState) {
        assertEquals(new FlashCardDetailId(1L, 1L), detail.getId());
        assertSame(flashCard, detail.getFlashCard());
        assertSame(deck, detail.getDeck());
        assertEquals(result.getStability(), detail.getStability());
        assertEquals(result.getDifficulty(), detail.getDifficulty());
        assertEquals(result.getNextReviewAt().toLocalDate(), detail.getNextReviewDate());
        assertEquals(result.getLastReviewAt().toLocalDate(), detail.getLastReviewDate());
        assertEquals(expectedLapses, detail.getLapses());
        assertEquals(expectedState, detail.getFsrsState());
    }
}
