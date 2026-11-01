package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.services.FlashCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/flashcards")
public class FlashCardController {
    private final FlashCardService flashCardService;

    @GetMapping("/me")
    public ApiResponse<List<FlashCardResponse>> getMyFlashCard() {
        return ApiResponse.<List<FlashCardResponse>>builder()
                .data(flashCardService.getMyFlashCard())
                .build();
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createFlashCard(@Valid @RequestBody NewFlashCardRequest request) {
        flashCardService.createFlashCard(request);
        return ApiResponse.<Void>builder()
                .message("Flashcard created successfully!")
                .build();
    }

    @PostMapping("/review")
    public ApiResponse<FSRSResult> reviewFlashCard(@Valid @RequestBody FlashCardRequest request) {
        FSRSResult result = flashCardService.reviewFlashCard(request);
        return ApiResponse.<FSRSResult>builder()
                .message("Flashcard reviewed successfully!")
                .data(result)
                .build();
    }
}
