package backend.dev.PLA.dto.request.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Body của POST /api/v1/questions/{questionId}/options và PUT /api/v1/options/{id}. questionId lấy từ path (POST)
 * hoặc từ đáp án hiện có (PUT), không nhận từ body.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptionRequest {
    @Min(value = 1, message = "OPTION_POSITION_INVALID")
    @Max(value = Short.MAX_VALUE, message = "OPTION_POSITION_INVALID")
    @Schema(
            nullable = true,
            minimum = "1",
            description = "Bỏ trống: POST xếp cuối (max + 1), PUT giữ nguyên vị trí cũ. Trùng đáp án khác → 400")
    Integer position;

    @NotBlank(message = "OPTION_CONTENT_BLANK")
    @Schema(example = "goes")
    String content;

    // Boolean (không phải boolean) để Lombok sinh getIsCorrect() → JSON đúng tên "isCorrect"
    @NotNull(message = "ANSWER_BLANK")
    @Schema(example = "true", description = "true thì các đáp án khác của câu tự về false")
    Boolean isCorrect;
}
