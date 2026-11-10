package backend.dev.PLA.dto.request.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Body của POST/PUT /api/v1/quizzes. Các field server tự gán (id, status, createdBy, createdAt) không có ở đây,
 * client có gửi thì bị bỏ qua.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizRequest {
    @NotBlank(message = "QUIZ_TITLE_BLANK")
    @Schema(example = "Present Simple - Daily routines", maxLength = 200)
    String title;

    @Schema(example = "Luyện thì hiện tại đơn qua chủ đề sinh hoạt hằng ngày", nullable = true)
    String description;

    @NotNull(message = "SKILL_BLANK")
    @Schema(example = "1")
    Long skillId;

    @Schema(example = "12", nullable = true, description = "Có topicId thì không được có customTopic")
    Long topicId;

    @Size(max = 200, message = "CUSTOM_TOPIC_SIZE")
    @Schema(nullable = true, description = "Chủ đề tự nhập khi không có topicId")
    String customTopic;

    @Schema(example = "3", nullable = true, description = "Phải thuộc skill đã chọn")
    Short formatId;

    @NotNull(message = "QUIZ_DIFFICULTY_BLANK")
    @Min(value = 1, message = "QUIZ_DIFFICULTY_OUT_OF_RANGE")
    @Max(value = 5, message = "QUIZ_DIFFICULTY_OUT_OF_RANGE")
    @Schema(example = "2", minimum = "1", maximum = "5")
    Short difficulty;

    // title 1-200 ký tự sau khi trim (@NotBlank đã chặn trường hợp rỗng)
    @JsonIgnore
    @AssertTrue(message = "QUIZ_TITLE_SIZE")
    public boolean isTitleSizeValid() {
        return title == null || title.trim().length() <= 200;
    }

    // Tương ứng chk_quiz_topic, chặt hơn: đúng một trong hai
    @JsonIgnore
    @AssertTrue(message = "QUIZ_TOPIC_EXACTLY_ONE")
    public boolean isExactlyOneTopic() {
        boolean hasCustomTopic = customTopic != null && !customTopic.isBlank();
        return (topicId != null) != hasCustomTopic;
    }
}
