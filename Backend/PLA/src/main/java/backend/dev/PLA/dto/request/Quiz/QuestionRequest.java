package backend.dev.PLA.dto.request.Quiz;

import backend.dev.PLA.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Dữ liệu tạo câu hỏi cho QuestionService.createQuestion (quizId truyền riêng). Không còn endpoint POST/PUT: câu hỏi
 * do AI sinh, được GeneratedQuizSaveService chuyển sang DTO này rồi lưu. Annotation validation giữ lại để mô tả ràng
 * buộc của câu hỏi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRequest {
    static final int CORRECT_TEXT_MAX_LENGTH = 255;

    @Schema(
            example = "7",
            nullable = true,
            description = "Passage cùng quiz. null = câu độc lập (câu do AI sinh luôn thuộc một passage)")
    Long passageId;

    @Min(value = 1, message = "QUESTION_POSITION_INVALID")
    @Max(value = Short.MAX_VALUE, message = "QUESTION_POSITION_INVALID")
    @Schema(
            nullable = true,
            minimum = "1",
            description = "Bỏ trống: xếp cuối (max + 1). Trùng câu khác → QUESTION_POSITION_DUPLICATED")
    Integer position;

    @NotNull(message = "QUESTION_TYPE_BLANK")
    @Schema(example = "SINGLE_CHOICE")
    QuestionType type;

    @NotBlank(message = "QUESTION_CONTENT_BLANK")
    @Schema(example = "What time does Tom get up?")
    String content;

    @Schema(
            nullable = true,
            maxLength = CORRECT_TEXT_MAX_LENGTH,
            description = "Bắt buộc với FILL_BLANK, phải bỏ trống với SINGLE_CHOICE")
    String correctText;

    @Schema(example = "Đoạn văn nói Tom dậy lúc 6 giờ.", nullable = true)
    String explanation;

    @Schema(example = "40", nullable = true, description = "Topic thuộc nhóm GRAMMAR")
    Long grammarTopicId;

    // FILL_BLANK phải có correctText, SINGLE_CHOICE không được có (chuỗi rỗng coi như không có)
    @JsonIgnore
    @AssertTrue(message = "QUESTION_CORRECT_TEXT_INVALID")
    public boolean isCorrectTextValid() {
        if (type == null) return true;
        boolean present = correctText != null && !correctText.isBlank();
        return type == QuestionType.FILL_BLANK ? present : !present;
    }

    // correctText tối đa 255 ký tự sau trim (cột VARCHAR(255))
    @JsonIgnore
    @AssertTrue(message = "CORRECT_TEXT_SIZE")
    public boolean isCorrectTextSizeValid() {
        return correctText == null || correctText.trim().length() <= CORRECT_TEXT_MAX_LENGTH;
    }
}
