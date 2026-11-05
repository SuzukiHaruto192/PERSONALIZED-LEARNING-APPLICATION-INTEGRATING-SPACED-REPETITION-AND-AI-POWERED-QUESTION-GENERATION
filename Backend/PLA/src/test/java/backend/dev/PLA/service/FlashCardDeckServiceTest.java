package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardDeckMapper;
import backend.dev.PLA.mapper.FlashCardDeckMapperImpl;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.mapper.FlashCardMapperImpl;
import backend.dev.PLA.repositories.flashcard.FlashCardDeckRepository;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
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
public class FlashCardDeckServiceTest {
    @Mock
    FlashCardDeckRepository flashCardDeckRepository;

    @Mock
    FlashCardRepository flashCardRepository;

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
        when(flashCardRepository.findAllByDeckId(5L))
                .thenReturn(List.of(FlashCard.builder()
                        .id(1L)
                        .frontContent("apple")
                        .backContent("tao")
                        .build()));

        List<FlashCardResponse> response = flashCardDeckService.getDeckFlashCards(customUserDetail, 5L);

        assertEquals(1, response.size());
        assertEquals("apple", response.getFirst().getFrontContent());
    }

    @Test
    void getDeckFlashCards_deckOfAnotherUser() {
        when(flashCardDeckRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> flashCardDeckService.getDeckFlashCards(customUserDetail, 5L));

        assertEquals(ErrorCode.DECK_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(flashCardRepository);
    }
}
