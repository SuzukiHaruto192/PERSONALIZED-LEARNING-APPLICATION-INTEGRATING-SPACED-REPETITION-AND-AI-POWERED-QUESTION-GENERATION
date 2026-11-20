package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.dto.request.flashcard.AddFlashCardsRequest;
import backend.dev.PLA.dto.request.flashcard.DeckFlashCardSearchRequest;
import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import backend.dev.PLA.services.flashcard.FlashCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class FlashCardDeckControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlashCardDeckService flashCardDeckService;

    @MockitoBean
    private FlashCardService flashCardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetail customUserDetail;
    private FlashCardDeckResponse deckResponse;

    @BeforeEach
    void initData() {
        customUserDetail = new CustomUserDetail(
                User.builder().id(1L).username("test").password("123456").build());
        deckResponse = FlashCardDeckResponse.builder()
                .id(5L)
                .name("IELTS")
                .description("tu vung")
                .createdAt(Instant.parse("2026-01-01T08:00:00Z"))
                .build();
    }

    @Test
    void getMyDecks_success() throws Exception {
        Mockito.when(flashCardDeckService.getMyDecks(any(CustomUserDetail.class)))
                .thenReturn(List.of(deckResponse));

        mockMvc.perform(get("/api/v1/decks").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].name").value("IELTS"))
                .andExpect(jsonPath("$.data[0].description").value("tu vung"))
                .andExpect(jsonPath("$.data[0].createdAt").exists());
    }

    @Test
    void getMyDecks_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/decks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));

        Mockito.verifyNoInteractions(flashCardDeckService);
    }

    @Test
    void createDeck_success() throws Exception {
        NewDeckRequest request =
                NewDeckRequest.builder().name("IELTS").description("tu vung").build();
        Mockito.when(flashCardDeckService.createDeck(any(CustomUserDetail.class), eq(request)))
                .thenReturn(deckResponse);

        mockMvc.perform(post("/api/v1/decks")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Deck created successfully!"))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void createDeck_blankName() throws Exception {
        mockMvc.perform(post("/api/v1/decks")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                NewDeckRequest.builder().name(" ").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("DECK_NAME_BLANK"));

        Mockito.verifyNoInteractions(flashCardDeckService);
    }

    @Test
    void getDeckFlashCards_success() throws Exception {
        PageResponse<DeckFlashCardResponse> page = PageResponse.<DeckFlashCardResponse>builder()
                .content(List.of(DeckFlashCardResponse.builder()
                        .deckId(5L)
                        .flashCardId(1L)
                        .frontContent("apple")
                        .backContent("tao")
                        .fsrsState(FsrsState.NEW)
                        .build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();
        Mockito.when(flashCardDeckService.getDeckFlashCards(
                        any(CustomUserDetail.class), eq(5L), any(DeckFlashCardSearchRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/decks/5/flashcards").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].frontContent").value("apple"))
                .andExpect(jsonPath("$.data.content[0].fsrsState").value("NEW"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getDeckFlashCards_deckNotFound() throws Exception {
        Mockito.when(flashCardDeckService.getDeckFlashCards(
                        any(CustomUserDetail.class), eq(5L), any(DeckFlashCardSearchRequest.class)))
                .thenThrow(new AppException(ErrorCode.DECK_NOT_FOUND));

        mockMvc.perform(get("/api/v1/decks/5/flashcards").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.DECK_NOT_FOUND.getCode()));
    }

    @Test
    void getDeckFlashCards_invalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/decks/5/flashcards")
                        .param("page", "-1")
                        .param("size", "101")
                        .with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").value("PAGE_INVALID"))
                .andExpect(jsonPath("$.errors.size").value("PAGE_SIZE_INVALID"));

        Mockito.verifyNoInteractions(flashCardDeckService);
    }

    @Test
    void updateDeck_nameExisted() throws Exception {
        NewDeckRequest request = NewDeckRequest.builder().name("TOEIC").build();
        Mockito.when(flashCardDeckService.updateDeck(any(CustomUserDetail.class), eq(5L), eq(request)))
                .thenThrow(new AppException(ErrorCode.DECK_EXISTED));

        mockMvc.perform(put("/api/v1/decks/5")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.DECK_EXISTED.getCode()));
    }

    @Test
    void deleteDeck_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/decks/5").with(user(customUserDetail))).andExpect(status().isNoContent());

        Mockito.verify(flashCardDeckService).deleteDeck(any(CustomUserDetail.class), eq(5L));
    }

    @Test
    void addExistingFlashCards_emptyIds() throws Exception {
        mockMvc.perform(post("/api/v1/decks/5/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashCardIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.flashCardIds").value("FLASHCARD_IDS_EMPTY"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void addExistingFlashCards_tooManyIds() throws Exception {
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        mockMvc.perform(post("/api/v1/decks/5/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AddFlashCardsRequest.builder().flashCardIds(ids).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.flashCardIds").value("FLASHCARD_IDS_TOO_MANY"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void removeFromDeck_detailNotFound() throws Exception {
        Mockito.doThrow(new AppException(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND))
                .when(flashCardService)
                .removeFromDeck(any(CustomUserDetail.class), eq(5L), eq(1L));

        mockMvc.perform(delete("/api/v1/decks/5/flashcards/1").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND.getCode()));
    }
}
