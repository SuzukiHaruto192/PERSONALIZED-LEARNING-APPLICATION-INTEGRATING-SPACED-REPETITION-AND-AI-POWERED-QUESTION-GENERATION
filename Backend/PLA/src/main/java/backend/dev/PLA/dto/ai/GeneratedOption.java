package backend.dev.PLA.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một đáp án của câu SINGLE_CHOICE do LLM sinh. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedOption {
    @JsonPropertyDescription("Nội dung đáp án bằng tiếng Anh")
    String content;

    // Boolean để JSON đúng tên "isCorrect"
    @JsonPropertyDescription("true nếu là đáp án đúng; mỗi câu đúng 1 đáp án true")
    Boolean isCorrect;
}
