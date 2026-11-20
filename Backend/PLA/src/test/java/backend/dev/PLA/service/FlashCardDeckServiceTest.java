package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.request.flashcard.DeckFlashCardSearchRequest;
import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckDetailResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.flashcard.FlashCardDetailId;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardDeckMapper;
import backend.dev.PLA.mapper.FlashCardDeckMapperImpl;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.mapper.FlashCardMapperImpl;
import backend.dev.PLA.repositories.flashcard.DeckStatsView;
import backend.dev.PLA.repositories.flashcard.FlashCardDeckRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardDetailRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import java.time.LocalDate;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
public class FlashCardDeckServiceTest {
    @Mock
    FlashCardDeckRepository flashCardDeckRepository;

    @Mock
    FlashCardRepository flashCardRepository;

    @Mock
    FlashCardDetailRepository flashCardDetailRepository;

    @Mock
    UserRepository userRepository;

    @Spy
    FlashCardDeckMapper flashCardDeckMapper = new FlashCardDeckMapperImpl();

    @Spy
    FlashCardMapper flashCardMapper = new FlashCardMapperImpl();

    @InjectMocks
    FlashCardDeckService flashCardDeckService;

    private User user;
    private CustomUserDetail customUserDetail;
    private FlashCardDeck deck;

    @BeforeEach
    void initData() {
        user = User.builder().id(1L).username("test").password("123456").build();
        customUserDetail = new CustomUserDetail(user);
        deck = FlashCardDeck.builder()
                .id(5L)
                .user(user)
                .name("IELTS")
                .description("tu vung")
                .build();
    }

    @Test
    void getMyDecks_success() {
        when(flashCardDeckRepository.findAllByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(deck));

        List<FlashCardDeckResponse> response = flashCardDeckService.getMyDecks(customUserDetail);

        assertEquals(1, response.size());
        assertEquals(5L, response.getFirst().getId());
        assertEquals("IELTS", response.getFirst().getName());
        assertEquals("tu vung", response.getFirst().getDescription());
    }

    @Test
    void createDeck_trimsNameAndAssignsUser() {
        when(flashCardDeckRepository.existsByUser_IdAndName(1L, "IELTS")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(flashCardDeckRepository.save(any(FlashCardDeck.class))).thenReturn(deck);

        FlashCardDeckResponse response = flashCardDeckService.createDeck(
                customUserDetail,
                NewDeckRequest.builder().name("  IELTS ").description("tu vung").build());

        ArgumentCaptor<FlashCardDeck> captor = ArgumentCaptor.forClass(FlashCardDeck.class);
        verify(flashCardDeckRepository).save(captor.capture());
        assertEquals("IELTS", captor.getValue().getName());
        assertSame(user, captor.getValue().getUser());
        assertEquals(5L, response.getId());
    }

    @Test
    void createDeck_nameExisted() {
        when(flashCardDeckRepository.existsByUser_IdAndName(1L, "IELTS")).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardDeckService.createDeck(
                        customUserDetail, NewDeckRequest.builder().name("IELTS").build()));

        assertEquals(ErrorCode.DECK_EXISTED, exception.getErrorCode());
        verify(flashCardDeckRepository, never()).save(any());
    }

    @Test
    void getDeckFlashCards_success() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        FlashCardDetail detail = FlashCardDetail.builder()
                .id(new FlashCardDetailId(5L, 1L))
                .deck(deck)
                .flashCard(FlashCard.builder()
                        .id(1L)
                        .frontContent("apple")
                        .backContent("tao")
                        .build())
                .fsrsState(FsrsState.REVIEW)
                .nextReviewDate(LocalDate.now().minusDays(2))
                .build();
        when(flashCardDetailRepository.findPageByDeckId(5L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(detail), PageRequest.of(0, 20), 1));

        PageResponse<DeckFlashCardResponse> response =
                flashCardDeckService.getDeckFlashCards(customUserDetail, 5L, new DeckFlashCardSearchRequest());

        assertEquals(1, response.getTotalElements());
        DeckFlashCardResponse card = response.getContent().getFirst();
        assertEquals("apple", card.getFrontContent());
        assertEquals("IELTS", card.getDeckName());
        assertEquals(FsrsState.REVIEW, card.getFsrsState());
        assertEquals(2, card.getOverdueDays());
    }

    @Test
    void getDeckFlashCards_deckOfAnotherUser() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardDeckService.getDeckFlashCards(customUserDetail, 5L, new DeckFlashCardSearchRequest()));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(flashCardDetailRepository);
    }

    // ===================== detail =====================

    @Test
    void getDeckDetail_success() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        DeckStatsView stats = mock(DeckStatsView.class);
        when(stats.getTotalCards()).thenReturn(60L);
        when(stats.getDueToday()).thenReturn(8L);
        when(stats.getNewCards()).thenReturn(12L);
        when(stats.getLearning()).thenReturn(5L);
        when(stats.getReview()).thenReturn(40L);
        when(stats.getRelearning()).thenReturn(3L);
        when(flashCardDetailRepository.countStatsByDeck(eq(5L), any(LocalDate.class)))
                .thenReturn(stats);

        FlashCardDeckDetailResponse response = flashCardDeckService.getDeckDetail(customUserDetail, 5L);

        assertEquals(5L, response.getId());
        assertEquals("IELTS", response.getName());
        assertEquals(60L, response.getStats().getTotalCards());
        assertEquals(8L, response.getStats().getDueToday());
        assertEquals(12L, response.getStats().getNewCards());
        assertEquals(3L, response.getStats().getRelearning());
    }

    @Test
    void getDeckDetail_deckOfAnotherUser() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> flashCardDeckService.getDeckDetail(customUserDetail, 5L));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
    }

    // ===================== update =====================

    @Test
    void updateDeck_success() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        when(flashCardDeckRepository.existsByUser_IdAndNameAndIdNot(1L, "TOEIC", 5L))
                .thenReturn(false);
        when(flashCardDeckRepository.saveAndFlush(deck)).thenReturn(deck);

        FlashCardDeckResponse response = flashCardDeckService.updateDeck(
                customUserDetail,
                5L,
                NewDeckRequest.builder().name(" TOEIC ").description("moi").build());

        assertEquals("TOEIC", response.getName());
        assertEquals("moi", response.getDescription());
    }

    @Test
    void updateDeck_nameExisted() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        when(flashCardDeckRepository.existsByUser_IdAndNameAndIdNot(1L, "TOEIC", 5L))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardDeckService.updateDeck(
                        customUserDetail,
                        5L,
                        NewDeckRequest.builder().name("TOEIC").build()));

        assertEquals(ErrorCode.DECK_EXISTED, exception.getErrorCode());
        verify(flashCardDeckRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDeck_concurrentDuplicate_mapsToDeckExisted() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        when(flashCardDeckRepository.existsByUser_IdAndNameAndIdNot(1L, "TOEIC", 5L))
                .thenReturn(false);
        when(flashCardDeckRepository.saveAndFlush(deck)).thenThrow(new DataIntegrityViolationException("dup"));

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardDeckService.updateDeck(
                        customUserDetail,
                        5L,
                        NewDeckRequest.builder().name("TOEIC").build()));

        assertEquals(ErrorCode.DECK_EXISTED, exception.getErrorCode());
    }

    @Test
    void updateDeck_deckOfAnotherUser() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> flashCardDeckService.updateDeck(
                        customUserDetail,
                        5L,
                        NewDeckRequest.builder().name("TOEIC").build()));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
    }

    // ===================== delete =====================

    @Test
    void deleteDeck_success_cleansOrphans() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        when(flashCardDetailRepository.findFlashCardIdsByDeckId(5L)).thenReturn(List.of(1L, 2L));

        flashCardDeckService.deleteDeck(customUserDetail, 5L);

        verify(flashCardDeckRepository).deleteByIdDirectly(5L);
        verify(flashCardRepository).deleteOrphansIn(List.of(1L, 2L));
    }

    @Test
    void deleteDeck_emptyDeck_skipsOrphanCleanup() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(deck));
        when(flashCardDetailRepository.findFlashCardIdsByDeckId(5L)).thenReturn(List.of());

        flashCardDeckService.deleteDeck(customUserDetail, 5L);

        verify(flashCardDeckRepository).deleteByIdDirectly(5L);
        verifyNoInteractions(flashCardRepository);
    }

    @Test
    void deleteDeck_deckOfAnotherUser() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> flashCardDeckService.deleteDeck(customUserDetail, 5L));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
        verify(flashCardDeckRepository, never()).deleteByIdDirectly(any());
    }
}
