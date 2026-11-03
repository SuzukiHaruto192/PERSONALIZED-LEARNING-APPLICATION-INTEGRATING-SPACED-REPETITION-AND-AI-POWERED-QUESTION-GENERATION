package backend.dev.PLA.controller;

import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FlashCardDeckControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlashCardDeckService flashCardDeckService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetail customUserDetail;
    private FlashCardDeckResponse deckResponse;

    @BeforeEach
    void initData() {
        customUserDetail = new CustomUserDetail(User.builder()
                .id(1L)
                .username("test")
                .password("123456")
                .build());
        deckResponse = FlashCardDeckResponse.builder()
                .id(5L)
                .name("IELTS")
                .description("tu vung")
                .createdAt(Instant.parse("2026-01-01T08:00:00Z"))
                .build();
    }

    @Test
    void getMyDecks_success() throws Exception {
        Mockito.when(flashCardDeckService.getMyDecks(any(CustomUserDetail.class))).thenReturn(List.of(deckResponse));

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
        NewDeckRequest request = NewDeckRequest.builder().name("IELTS").description("tu vung").build();
        Mockito.when(flashCardDeckService.createDeck(any(CustomUserDetail.class), eq(request))).thenReturn(deckResponse);

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
                        .content(objectMapper.writeValueAsString(NewDeckRequest.builder().name(" ").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("DECK_NAME_BLANK"));

        Mockito.verifyNoInteractions(flashCardDeckService);
    }

    @Test
    void getDeckFlashCards_success() throws Exception {
        Mockito.when(flashCardDeckService.getDeckFlashCards(any(CustomUserDetail.class), eq(5L)))
                .thenReturn(List.of(FlashCardResponse.builder().id(1L).frontContent("apple").backContent("tao").build()));

        mockMvc.perform(get("/api/v1/decks/5/flashcards").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].frontContent").value("apple"));
    }

    @Test
    void getDeckFlashCards_deckNotFound() throws Exception {
        Mockito.when(flashCardDeckService.getDeckFlashCards(any(CustomUserDetail.class), eq(5L)))
                .thenThrow(new AppException(ErrorCode.DECK_NOT_FOUND));

        mockMvc.perform(get("/api/v1/decks/5/flashcards").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.DECK_NOT_FOUND.getCode()));
    }
}
