package backend.dev.PLA.dto.request.Quiz;

import backend.dev.PLA.enums.QuizStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Query params của GET /api/v1/quizzes: phân trang + lọc. Field lọc null = không lọc. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSearchRequest {
    @Min(value = 0, message = "PAGE_INVALID")
    @Schema(defaultValue = "0", minimum = "0")
    @Builder.Default
    int page = 0;

    @Min(value = 1, message = "PAGE_SIZE_INVALID")
    @Max(value = 100, message = "PAGE_SIZE_INVALID")
    @Schema(defaultValue = "20", minimum = "1", maximum = "100")
    @Builder.Default
    int size = 20;

    @Schema(example = "1")
    Long skillId;

    Long topicId;

    Short formatId;

    @Min(value = 1, message = "QUIZ_DIFFICULTY_OUT_OF_RANGE")
    @Max(value = 5, message = "QUIZ_DIFFICULTY_OUT_OF_RANGE")
    Short difficulty;

    QuizStatus status;

    @Schema(example = "present", description = "Tìm trong title, không phân biệt hoa thường")
    String keyword;

    @Schema(
            defaultValue = "createdAt,desc",
            description = "Dạng field,asc|desc với field là createdAt, title, difficulty")
    @Builder.Default
    String sort = "createdAt,desc";
}
