package backend.dev.PLA.controllers.flashcard;

import backend.dev.PLA.dto.request.flashcard.DueFlashCardSearchRequest;
import backend.dev.PLA.dto.request.flashcard.FlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.request.flashcard.UpdateFlashCardRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.security.CustomUserDetail;
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
@RequestMapping("/api/v1")
@Tag(name = "Flashcard", description = "API thẻ ghi nhớ và phiên ôn tập FSRS của user hiện tại")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardController {
    FlashCardService flashCardService;

    @GetMapping("/flashcards")
    @Operation(
            summary = "Tất cả thẻ của tôi",
            description = "Mọi thẻ nằm trong các deck của tôi (thẻ nằm ở nhiều deck chỉ trả 1 lần).")
    public ResponseEntity<ApiResponse<List<FlashCardResponse>>> getMyFlashCard(
            @AuthenticationPrincipal CustomUserDetail userDetail) {
        return ResponseEntity.ok(ApiResponse.<List<FlashCardResponse>>builder()
                .data(flashCardService.getMyFlashCard(userDetail))
                .build());
    }

    @PostMapping("/flashcards")
    @Operation(
            summary = "Tạo thẻ vào một deck",
            description = "Thẻ trùng nội dung (không phân biệt hoa thường) do chính tôi tạo thì dùng lại, không tạo "
                    + "thẻ mới. Thẻ đã có trong deck → FLASHCARD_DETAIL_EXISTED (1028).")
    public ResponseEntity<ApiResponse<Void>> createFlashCard(
            @AuthenticationPrincipal CustomUserDetail userDetail, @Valid @RequestBody NewFlashCardRequest request) {
        flashCardService.createFlashCard(userDetail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .message("Flashcard created successfully!")
                        .build());
    }

    @GetMapping("/flashcards/due")
    @Operation(
            summary = "Thẻ đến hạn ôn",
            description = "Tất cả thẻ đến hạn (next_review <= hôm nay) có phân trang, quá hạn lâu nhất đứng trước. "
                    + "Không gồm thẻ mới chưa ôn lần nào. Bỏ deckId để lấy mọi deck. Lỗi: 1006 với "
                    + "errors.page = PAGE_INVALID / errors.size = PAGE_SIZE_INVALID, "
                    + "deck không phải của tôi → DECK_NOT_FOUND (1046).")
    public ResponseEntity<ApiResponse<PageResponse<DeckFlashCardResponse>>> getDueFlashCards(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @ParameterObject @Validated @ModelAttribute DueFlashCardSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(flashCardService.getDueFlashCards(userDetail, request)));
    }

    @PostMapping("/flashcards/review")
    @Operation(
            summary = "Chấm điểm 1 thẻ trong 1 deck",
            description = "rating 1 Again / 2 Hard / 3 Good / 4 Easy. Tính lại lịch ôn FSRS và trả trạng thái mới "
                    + "của thẻ trong deck.")
    public ResponseEntity<ApiResponse<DeckFlashCardResponse>> reviewFlashCard(
            @AuthenticationPrincipal CustomUserDetail userDetail, @Valid @RequestBody FlashCardRequest request) {
        return ResponseEntity.ok(ApiResponse.<DeckFlashCardResponse>builder()
                .message("Flashcard reviewed successfully!")
                .data(flashCardService.reviewFlashCard(userDetail, request))
                .build());
    }

    @PutMapping("/flashcards/{flashCardId}")
    @Operation(
            summary = "Sửa nội dung thẻ",
            description = "Không đổi lịch ôn. Thẻ không nằm trong deck nào của tôi → FLASHCARD_NOT_FOUND (1031); "
                    + "trùng nội dung thẻ khác của tôi → FLASHCARD_CONTENT_EXISTED (1121). Thẻ còn nằm trong deck "
                    + "của user khác thì tạo bản sao cho tôi (id trả về khác id cũ), user kia giữ nội dung cũ.")
    public ResponseEntity<ApiResponse<FlashCardResponse>> updateFlashCard(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long flashCardId,
            @Valid @RequestBody UpdateFlashCardRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(flashCardService.updateFlashCard(userDetail, flashCardId, request)));
    }

    @DeleteMapping("/flashcards/{flashCardId}")
    @Operation(
            summary = "Xoá thẻ khỏi mọi deck của tôi",
            description = "Dòng thẻ chỉ bị xoá hẳn khi không còn deck nào (của bất kỳ ai) chứa nó. "
                    + "Lỗi: FLASHCARD_NOT_FOUND (1031).")
    public ResponseEntity<Void> deleteFlashCard(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long flashCardId) {
        flashCardService.deleteFlashCard(userDetail, flashCardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/passages/{passageId}/flashcards")
    @Operation(
            summary = "Tạo thẻ từ passage đang đọc",
            description = "Dùng khi bôi đen từ lạ trong bài đọc. Thẻ lưu source_passage_id. Lỗi: PASSAGE_NOT_FOUND "
                    + "(1079), passage thuộc quiz của người khác → ACCESS_DENIED (1002), DECK_NOT_FOUND (1046), "
                    + "FLASHCARD_DETAIL_EXISTED (1028).")
    public ResponseEntity<ApiResponse<DeckFlashCardResponse>> createFlashCardFromPassage(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long passageId,
            @Valid @RequestBody NewFlashCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(flashCardService.createFlashCardFromPassage(userDetail, passageId, request)));
    }
}
