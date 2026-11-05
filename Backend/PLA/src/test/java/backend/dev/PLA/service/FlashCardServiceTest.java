package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.flashcard.FlashCardDetailId;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.mapper.FlashCardMapperImpl;
import backend.dev.PLA.repositories.flashcard.FlashCardDetailRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import backend.dev.PLA.services.flashcard.FlashCardService;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private FlashCardResponse flashCardResponse;

    private FlashCardRequest flashCardRequest;

    private NewFlashCardRequest newFlashCardRequest;

    private User user;

    private CustomUserDetail customUserDetail;

    private FlashCard flashCard;

    private FlashCardDeck deck;

    private FlashCardDetail flashCardDetail;

    @BeforeEach
    void initData() {
        flashCardResponse = FlashCardResponse.builder()
                .id(1L)
                .frontContent("apple")
                .backContent("táo")
                .build();
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
                .difficulty(0.0)
                .lapses(0)
                .lastReviewDate(null)
                .nextReviewDate(null)
                .deck(deck)
                .flashCard(flashCard)
                .stability(0.0)
                .build();
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

    @Test
    public void createFlashCardTest_newCard() {
        NewFlashCardRequest request = NewFlashCardRequest.builder()
                .deckId(1L)
                .frontContent("  apple  ")
                .backContent("  táo  ")
                .build();
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardRepository.findByContent("apple", "táo")).thenReturn(null);
        when(flashCardRepository.save(any(FlashCard.class))).thenReturn(flashCard);

        flashCardService.createFlashCard(customUserDetail, request);

        ArgumentCaptor<FlashCard> cardCaptor = ArgumentCaptor.forClass(FlashCard.class);
        verify(flashCardRepository).save(cardCaptor.capture());
        assertEquals("apple", cardCaptor.getValue().getFrontContent());
        assertEquals("táo", cardCaptor.getValue().getBackContent());
        assertSame(user, cardCaptor.getValue().getCreatedBy());

        assertInitialDetail(captureSavedDetail());
        verify(flashCardDetailRepository, never()).existsById(any());
    }

    @Test
    public void createFlashCardTest_existingCardNotInUserCollection() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardRepository.findByContent("apple", "táo")).thenReturn(flashCard);
        when(flashCardDetailRepository.existsById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(false);

        flashCardService.createFlashCard(customUserDetail, newFlashCardRequest);

        verify(flashCardRepository, never()).save(any());
        assertInitialDetail(captureSavedDetail());
    }

    @Test
    public void createFlashCardTest_existingCardAlreadyInUserCollection() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardRepository.findByContent("apple", "táo")).thenReturn(flashCard);
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

        FSRSResult result = flashCardService.reviewFlashCard(customUserDetail, flashCardRequest);

        // Giá trị khởi tạo của FSRS với rating 3: S0 = w[2], D0 = w[4]
        assertEquals(3.7145, result.getStability());
        assertEquals(5.1618, result.getDifficulty());
        assertEquals(4, result.getIntervalDays());

        assertDetailMatchesResult(captureSavedDetail(), result, 0);
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

        FSRSResult result = flashCardService.reviewFlashCard(customUserDetail, flashCardRequest);

        assertSame(expected, result);
        assertDetailMatchesResult(captureSavedDetail(), expected, 2);
    }

    @Test
    public void reviewFlashCardTest_ratingAgainIncreasesLapses() {
        flashCardDetail.setLapses(2);
        flashCardDetail.setFsrsState(FsrsState.REVIEW);
        FlashCardRequest request =
                FlashCardRequest.builder().deckId(1L).flashCardId(1L).rating(1).build();
        when(flashCardDetailRepository.findById(new FlashCardDetailId(1L, 1L)))
                .thenReturn(Optional.of(flashCardDetail));

        FSRSResult result = flashCardService.reviewFlashCard(customUserDetail, request);

        assertDetailMatchesResult(captureSavedDetail(), result, 3, FsrsState.RELEARNING);
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
        assertEquals(0.0, detail.getStability());
        assertEquals(0.0, detail.getDifficulty());
        assertNull(detail.getNextReviewDate());
        assertNull(detail.getLastReviewDate());
        assertEquals(0, detail.getLapses());
        assertEquals(FsrsState.NEW, detail.getFsrsState());
    }

    private void assertDetailMatchesResult(FlashCardDetail detail, FSRSResult result, int expectedLapses) {
        assertDetailMatchesResult(detail, result, expectedLapses, FsrsState.REVIEW);
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
