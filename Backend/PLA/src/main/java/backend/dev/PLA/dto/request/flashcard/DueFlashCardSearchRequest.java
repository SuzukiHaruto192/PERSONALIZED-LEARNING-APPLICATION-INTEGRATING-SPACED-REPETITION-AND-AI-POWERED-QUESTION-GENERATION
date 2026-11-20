package backend.dev.PLA.dto.request.flashcard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Query params của GET /api/v1/flashcards/due: phân trang thẻ đến hạn, quá hạn lâu nhất đứng trước. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DueFlashCardSearchRequest {
    @Schema(description = "Chỉ ôn 1 deck; bỏ trống = mọi deck của tôi", example = "3")
    Long deckId;

    @Min(value = 0, message = "PAGE_INVALID")
    @Schema(defaultValue = "0", minimum = "0")
    @Builder.Default
    int page = 0;

    @Min(value = 1, message = "PAGE_SIZE_INVALID")
    @Max(value = 100, message = "PAGE_SIZE_INVALID")
    @Schema(defaultValue = "20", minimum = "1", maximum = "100")
    @Builder.Default
    int size = 20;
}
