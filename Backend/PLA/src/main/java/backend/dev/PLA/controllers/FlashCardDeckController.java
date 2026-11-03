package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/decks")
public class FlashCardDeckController {
    private final FlashCardDeckService flashCardDeckService;

    @GetMapping("")
    public ApiResponse<List<FlashCardDeckResponse>> getMyDecks(@AuthenticationPrincipal CustomUserDetail userDetail) {
        return ApiResponse.<List<FlashCardDeckResponse>>builder()
                .data(flashCardDeckService.getMyDecks(userDetail))
                .build();
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FlashCardDeckResponse> createDeck(@AuthenticationPrincipal CustomUserDetail userDetail,
                                                         @Valid @RequestBody NewDeckRequest request) {
        return ApiResponse.<FlashCardDeckResponse>builder()
                .message("Deck created successfully!")
                .data(flashCardDeckService.createDeck(userDetail, request))
                .build();
    }

    @GetMapping("/{deckId}/flashcards")
    public ApiResponse<List<FlashCardResponse>> getDeckFlashCards(@AuthenticationPrincipal CustomUserDetail userDetail,
                                                                  @PathVariable Long deckId) {
        return ApiResponse.<List<FlashCardResponse>>builder()
                .data(flashCardDeckService.getDeckFlashCards(userDetail, deckId))
                .build();
    }
}
