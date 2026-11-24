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
 * Dữ liệu tạo đáp án cho OptionService.createOption (questionId truyền riêng). Không còn endpoint POST/PUT: đáp án do
 * AI sinh, được GeneratedQuizSaveService chuyển sang DTO này rồi lưu. Annotation validation giữ lại để mô tả ràng buộc
 * của đáp án.
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
            description = "Bỏ trống: xếp cuối (max + 1). Trùng đáp án khác → OPTION_POSITION_DUPLICATED")
    Integer position;

    @NotBlank(message = "OPTION_CONTENT_BLANK")
    @Schema(example = "goes")
    String content;

    // Boolean (không phải boolean) để Lombok sinh getIsCorrect() → JSON đúng tên "isCorrect"
    @NotNull(message = "ANSWER_BLANK")
    @Schema(example = "true", description = "true thì các đáp án khác của câu tự về false")
    Boolean isCorrect;
}
