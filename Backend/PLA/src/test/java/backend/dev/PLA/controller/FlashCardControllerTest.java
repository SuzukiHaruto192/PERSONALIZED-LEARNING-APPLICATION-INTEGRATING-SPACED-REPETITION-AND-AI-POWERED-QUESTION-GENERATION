package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.UpdateFlashCardRequest;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class FlashCardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlashCardService flashCardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FlashCardResponse flashCardResponse;
    private NewFlashCardRequest newFlashCardRequest;
    private FlashCardRequest flashCardRequest;
    private CustomUserDetail customUserDetail;

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
        User user = User.builder()
                .id(1L)
                .username("test")
                .email("test@gmail.com")
                .password("123456")
                .build();
        customUserDetail = new CustomUserDetail(user);
    }

    @Test
    void getFlashCard_success() throws Exception {
        List<FlashCardResponse> flashCardResponseList = Arrays.asList(flashCardResponse);

        Mockito.when(flashCardService.getMyFlashCard(customUserDetail)).thenReturn(flashCardResponseList);

        mockMvc.perform(get("/api/v1/flashcards").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].frontContent").value("apple"))
                .andExpect(jsonPath("$.data[0].backContent").value("táo"));

        Mockito.verify(flashCardService).getMyFlashCard(any(CustomUserDetail.class));
    }

    @Test
    void getFlashCard_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/flashcards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()))
                .andExpect(jsonPath("$.path").value("/api/v1/flashcards"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void createFlashCard_success() throws Exception {
        String content = objectMapper.writeValueAsString(newFlashCardRequest);

        Mockito.doNothing()
                .when(flashCardService)
                .createFlashCard(any(CustomUserDetail.class), any(NewFlashCardRequest.class));

        mockMvc.perform(post("/api/v1/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Flashcard created successfully!"))
                .andExpect(jsonPath("$.data").doesNotExist());

        Mockito.verify(flashCardService).createFlashCard(customUserDetail, newFlashCardRequest);
    }

    @Test
    void createFlashCard_blankContent() throws Exception {
        String content = objectMapper.writeValueAsString(NewFlashCardRequest.builder()
                .deckId(1L)
                .frontContent(" ")
                .backContent("")
                .build());

        mockMvc.perform(post("/api/v1/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.frontContent").value("FRONT_CONTENT_BLANK"))
                .andExpect(jsonPath("$.errors.backContent").value("BACK_CONTENT_BLANK"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void createFlashCard_missingDeckId() throws Exception {
        newFlashCardRequest.setDeckId(null);
        String content = objectMapper.writeValueAsString(newFlashCardRequest);

        mockMvc.perform(post("/api/v1/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.deckId").value("DECK_ID_REQUIRED"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void createFlashCard_flashCardDetailExisted() throws Exception {
        String content = objectMapper.writeValueAsString(newFlashCardRequest);

        Mockito.doThrow(new AppException(ErrorCode.FLASHCARD_DETAIL_EXISTED))
                .when(flashCardService)
                .createFlashCard(any(CustomUserDetail.class), any(NewFlashCardRequest.class));

        mockMvc.perform(post("/api/v1/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_DETAIL_EXISTED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FLASHCARD_DETAIL_EXISTED.getMessage()))
                .andExpect(jsonPath("$.path").value("/api/v1/flashcards"));
    }

    @Test
    void createFlashCard_unauthenticated() throws Exception {
        String content = objectMapper.writeValueAsString(newFlashCardRequest);

        mockMvc.perform(post("/api/v1/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void reviewFlashCard_success() throws Exception {
        String content = objectMapper.writeValueAsString(flashCardRequest);
        DeckFlashCardResponse reviewed = DeckFlashCardResponse.builder()
                .deckId(1L)
                .flashCardId(1L)
                .frontContent("apple")
                .fsrsState(FsrsState.REVIEW)
                .lapses(0)
                .lastReviewDate(LocalDate.of(2026, 1, 1))
                .nextReviewDate(LocalDate.of(2026, 1, 4))
                .build();

        Mockito.when(flashCardService.reviewFlashCard(any(CustomUserDetail.class), any(FlashCardRequest.class)))
                .thenReturn(reviewed);

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Flashcard reviewed successfully!"))
                .andExpect(jsonPath("$.data.fsrsState").value("REVIEW"))
                .andExpect(jsonPath("$.data.nextReviewDate").value("2026-01-04"))
                .andExpect(jsonPath("$.data.lapses").value(0));

        Mockito.verify(flashCardService).reviewFlashCard(customUserDetail, flashCardRequest);
    }

    @Test
    void reviewFlashCard_missingFields() throws Exception {
        String content = objectMapper.writeValueAsString(new FlashCardRequest());

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.deckId").value("DECK_ID_REQUIRED"))
                .andExpect(jsonPath("$.errors.flashCardId").value("FLASHCARD_ID_REQUIRED"))
                .andExpect(jsonPath("$.errors.rating").value("RATING_REQUIRED"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void reviewFlashCard_ratingBelowMin() throws Exception {
        flashCardRequest.setRating(0);
        String content = objectMapper.writeValueAsString(flashCardRequest);

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.rating").value("RATING_MIN"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void reviewFlashCard_ratingAboveMax() throws Exception {
        flashCardRequest.setRating(5);
        String content = objectMapper.writeValueAsString(flashCardRequest);

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.rating").value("RATING_MAX"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void reviewFlashCard_flashCardNotFound() throws Exception {
        String content = objectMapper.writeValueAsString(flashCardRequest);

        Mockito.when(flashCardService.reviewFlashCard(any(CustomUserDetail.class), any(FlashCardRequest.class)))
                .thenThrow(new AppException(ErrorCode.FLASHCARD_NOT_FOUND));

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FLASHCARD_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.path").value("/api/v1/flashcards/review"));
    }

    @Test
    void createFlashCard_contentTooLong() throws Exception {
        newFlashCardRequest.setFrontContent("a".repeat(256));
        newFlashCardRequest.setBackContent("b".repeat(256));
        newFlashCardRequest.setExample("c".repeat(1001));

        mockMvc.perform(post("/api/v1/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newFlashCardRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.frontContent").value("FRONT_CONTENT_SIZE"))
                .andExpect(jsonPath("$.errors.backContent").value("BACK_CONTENT_SIZE"))
                .andExpect(jsonPath("$.errors.example").value("FLASHCARD_EXAMPLE_SIZE"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void getDueFlashCards_pageOutOfRange() throws Exception {
        mockMvc.perform(get("/api/v1/flashcards/due")
                        .param("page", "-1")
                        .param("size", "101")
                        .with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.page").value("PAGE_INVALID"))
                .andExpect(jsonPath("$.errors.size").value("PAGE_SIZE_INVALID"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void updateFlashCard_blankAndTooLong() throws Exception {
        UpdateFlashCardRequest request = UpdateFlashCardRequest.builder()
                .frontContent(" ")
                .backContent("b".repeat(256))
                .build();

        mockMvc.perform(put("/api/v1/flashcards/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.frontContent").value("FRONT_CONTENT_BLANK"))
                .andExpect(jsonPath("$.errors.backContent").value("BACK_CONTENT_SIZE"));

        Mockito.verifyNoInteractions(flashCardService);
    }

    @Test
    void updateFlashCard_contentExisted() throws Exception {
        UpdateFlashCardRequest request = UpdateFlashCardRequest.builder()
                .frontContent("pear")
                .backContent("lê")
                .build();
        Mockito.when(flashCardService.updateFlashCard(
                        any(CustomUserDetail.class), eq(1L), any(UpdateFlashCardRequest.class)))
                .thenThrow(new AppException(ErrorCode.FLASHCARD_CONTENT_EXISTED));

        mockMvc.perform(put("/api/v1/flashcards/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_CONTENT_EXISTED.getCode()));
    }

    @Test
    void deleteFlashCard_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/flashcards/1").with(user(customUserDetail)))
                .andExpect(status().isNoContent());

        Mockito.verify(flashCardService).deleteFlashCard(any(CustomUserDetail.class), eq(1L));
    }

    @Test
    void deleteFlashCard_notFound() throws Exception {
        Mockito.doThrow(new AppException(ErrorCode.FLASHCARD_NOT_FOUND))
                .when(flashCardService)
                .deleteFlashCard(any(CustomUserDetail.class), eq(1L));

        mockMvc.perform(delete("/api/v1/flashcards/1").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_NOT_FOUND.getCode()));
    }

    @Test
    void createFlashCardFromPassage_accessDenied() throws Exception {
        Mockito.when(flashCardService.createFlashCardFromPassage(
                        any(CustomUserDetail.class), eq(12L), any(NewFlashCardRequest.class)))
                .thenThrow(new AppException(ErrorCode.ACCESS_DENIED));

        mockMvc.perform(post("/api/v1/passages/12/flashcards")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newFlashCardRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
    }

    @Test
    void reviewFlashCard_unauthenticated() throws Exception {
        String content = objectMapper.writeValueAsString(flashCardRequest);

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));

        Mockito.verifyNoInteractions(flashCardService);
    }
}
