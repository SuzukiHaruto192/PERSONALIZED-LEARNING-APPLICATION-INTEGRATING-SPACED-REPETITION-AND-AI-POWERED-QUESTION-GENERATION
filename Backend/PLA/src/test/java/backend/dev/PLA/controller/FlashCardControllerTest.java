package backend.dev.PLA.controller;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
            flashCardRequest = FlashCardRequest.builder()
                    .deckId(1L)
                    .flashCardId(1L)
                    .rating(3)
                    .build();
        User user = User.builder()
                .id(1L)
                .username("test")
                .email("test@gmail.com")
                .password("123456")
                .build();
        customUserDetail = new CustomUserDetail(user);

    }
    @Test
    void getFlashCard_success() throws Exception  {
        List<FlashCardResponse> flashCardResponseList = Arrays.asList(flashCardResponse);

        Mockito.when(flashCardService.getMyFlashCard(customUserDetail)).thenReturn(flashCardResponseList);

        mockMvc.perform(get("/api/v1/flashcards")
                        .with(user(customUserDetail)))
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

        Mockito.doNothing().when(flashCardService)
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
        FSRSResult fsrsResult = FSRSResult.builder()
                .stability(2.5)
                .difficulty(4.5)
                .intervalDays(3)
                .nextReviewAt(LocalDateTime.of(2026, 1, 4, 8, 0))
                .lastReviewAt(LocalDateTime.of(2026, 1, 1, 8, 0))
                .build();

        Mockito.when(flashCardService.reviewFlashCard(any(CustomUserDetail.class), any(FlashCardRequest.class)))
                .thenReturn(fsrsResult);

        mockMvc.perform(post("/api/v1/flashcards/review")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Flashcard reviewed successfully!"))
                .andExpect(jsonPath("$.data.stability").value(2.5))
                .andExpect(jsonPath("$.data.difficulty").value(4.5))
                .andExpect(jsonPath("$.data.intervalDays").value(3));

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
