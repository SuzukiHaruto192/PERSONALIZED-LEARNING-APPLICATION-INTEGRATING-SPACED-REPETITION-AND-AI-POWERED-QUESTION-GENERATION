package backend.dev.PLA.controllers.flashcard;

import backend.dev.PLA.dto.request.flashcard.AddFlashCardsRequest;
import backend.dev.PLA.dto.request.flashcard.DeckFlashCardSearchRequest;
import backend.dev.PLA.dto.request.flashcard.NewDeckRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.flashcard.AddFlashCardsResponse;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckDetailResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.flashcard.FlashCardDeckService;
import backend.dev.PLA.services.flashcard.FlashCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/decks")
@Tag(name = "Flashcard deck", description = "API bộ thẻ (deck) của user hiện tại")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardDeckController {
    FlashCardDeckService flashCardDeckService;
    FlashCardService flashCardService;

    @GetMapping("")
    @Operation(summary = "Danh sách deck của tôi", description = "Deck tạo sau đứng trước.")
    public ResponseEntity<ApiResponse<List<FlashCardDeckResponse>>> getMyDecks(
            @AuthenticationPrincipal CustomUserDetail userDetail) {
        return ResponseEntity.ok(ApiResponse.<List<FlashCardDeckResponse>>builder()
                .data(flashCardDeckService.getMyDecks(userDetail))
                .build());
    }

    @PostMapping("")
    @Operation(summary = "Tạo deck", description = "Tên deck không trùng với deck khác của tôi (DECK_EXISTED 1045).")
    public ResponseEntity<ApiResponse<FlashCardDeckResponse>> createDeck(
            @AuthenticationPrincipal CustomUserDetail userDetail, @Valid @RequestBody NewDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<FlashCardDeckResponse>builder()
                        .message("Deck created successfully!")
                        .data(flashCardDeckService.createDeck(userDetail, request))
                        .build());
    }

    @GetMapping("/{deckId}")
    @Operation(
            summary = "Chi tiết deck kèm thống kê",
            description = "stats: totalCards, dueToday (next_review <= hôm nay), newCards, learning, review, "
                    + "relearning. Lỗi: DECK_NOT_FOUND (1046).")
    public ResponseEntity<ApiResponse<FlashCardDeckDetailResponse>> getDeckDetail(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long deckId) {
        return ResponseEntity.ok(ApiResponse.success(flashCardDeckService.getDeckDetail(userDetail, deckId)));
    }

    @PutMapping("/{deckId}")
    @Operation(
            summary = "Đổi tên, mô tả deck",
            description = "Tên mới trùng deck khác của tôi → DECK_EXISTED (1045); giữ nguyên tên thì cho qua. "
                    + "Lỗi: DECK_NOT_FOUND (1046).")
    public ResponseEntity<ApiResponse<FlashCardDeckResponse>> updateDeck(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long deckId,
            @Valid @RequestBody NewDeckRequest request) {
        return ResponseEntity.ok(ApiResponse.success(flashCardDeckService.updateDeck(userDetail, deckId, request)));
    }

    @DeleteMapping("/{deckId}")
    @Operation(
            summary = "Xoá deck",
            description = "Xoá deck cùng lịch ôn của các thẻ trong deck; thẻ không còn nằm trong deck nào bị xoá "
                    + "theo. Lỗi: DECK_NOT_FOUND (1046).")
    public ResponseEntity<Void> deleteDeck(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long deckId) {
        flashCardDeckService.deleteDeck(userDetail, deckId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{deckId}/flashcards")
    @Operation(
            summary = "Thẻ trong deck kèm lịch ôn",
            description = "Phân trang, thẻ thêm vào trước đứng trước. Lỗi: DECK_NOT_FOUND (1046), 1006 với "
                    + "errors.page = PAGE_INVALID / errors.size = PAGE_SIZE_INVALID.")
    public ResponseEntity<ApiResponse<PageResponse<DeckFlashCardResponse>>> getDeckFlashCards(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long deckId,
            @ParameterObject @Validated @ModelAttribute DeckFlashCardSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<DeckFlashCardResponse>>builder()
                .data(flashCardDeckService.getDeckFlashCards(userDetail, deckId, request))
                .build());
    }

    @PostMapping("/{deckId}/flashcards")
    @Operation(
            summary = "Thêm thẻ có sẵn vào deck",
            description = "1-100 id, mỗi id phải là thẻ đang nằm trong một deck của tôi (sai 1 id → "
                    + "FLASHCARD_NOT_FOUND 1031, không thêm thẻ nào). Thẻ đã có trong deck thì bỏ qua (skipped). "
                    + "Thẻ được thêm bắt đầu lịch ôn mới (NEW).")
    public ResponseEntity<ApiResponse<AddFlashCardsResponse>> addExistingFlashCards(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long deckId,
            @Valid @RequestBody AddFlashCardsRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(flashCardService.addExistingFlashCards(userDetail, deckId, request)));
    }

    @DeleteMapping("/{deckId}/flashcards/{flashCardId}")
    @Operation(
            summary = "Gỡ thẻ khỏi deck",
            description = "Mất lịch ôn của thẻ trong deck này; thẻ không còn nằm trong deck nào bị xoá theo. "
                    + "Lỗi: DECK_NOT_FOUND (1046), FLASHCARD_DETAIL_NOT_FOUND (1120).")
    public ResponseEntity<Void> removeFromDeck(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long deckId,
            @PathVariable Long flashCardId) {
        flashCardService.removeFromDeck(userDetail, deckId, flashCardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deckId}/flashcards/{flashCardId}/reset")
    @Operation(
            summary = "Học lại thẻ từ đầu",
            description = "fsrs_state = NEW, stability / difficulty / last_review / next_review = NULL, lapses = 0. "
                    + "Lỗi: DECK_NOT_FOUND (1046), FLASHCARD_DETAIL_NOT_FOUND (1120).")
    public ResponseEntity<ApiResponse<DeckFlashCardResponse>> resetFlashCard(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long deckId,
            @PathVariable Long flashCardId) {
        return ResponseEntity.ok(ApiResponse.success(flashCardService.resetFlashCard(userDetail, deckId, flashCardId)));
    }
}
