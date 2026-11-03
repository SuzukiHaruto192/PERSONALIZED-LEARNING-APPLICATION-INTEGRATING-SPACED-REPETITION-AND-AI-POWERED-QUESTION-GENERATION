package backend.dev.PLA.dto.request.question;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizRequest {
    @NotNull(message = "SKILL_BLANK")
    Short skillId;

    Long topicId;

    @Size(max = 200, message = "CUSTOM_TOPIC_SIZE")
    String customTopic;

    // NULL với Grammar
    Short formatId;

    // NULL thì lấy mặc định 2 (Vừa)
    @Min(value = 1, message = "QUIZ_DIFFICULTY_RANGE")
    @Max(value = 3, message = "QUIZ_DIFFICULTY_RANGE")
    Short difficulty;

    @NotBlank(message = "QUIZ_TITLE_BLANK")
    @Size(max = 200, message = "QUIZ_TITLE_SIZE")
    String title;

    String content;

    @Size(max = 500, message = "AUDIO_URL_SIZE")
    String audioUrl;

    // Tương ứng chk_quiz_topic: phải có topic có sẵn hoặc chủ đề tự nhập
    @JsonIgnore
    @AssertTrue(message = "TOPIC_BLANK")
    public boolean isTopicProvided() {
        return topicId != null || (customTopic != null && !customTopic.isBlank());
    }
}
