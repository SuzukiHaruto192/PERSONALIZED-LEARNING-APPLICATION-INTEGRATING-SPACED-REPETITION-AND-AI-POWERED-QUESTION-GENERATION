package backend.dev.PLA.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một đoạn văn do LLM sinh, chứa các câu hỏi của nó. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedPassage {
    @JsonPropertyDescription(
            "Reading/Listening: đoạn văn tiếng Anh. Grammar: hướng dẫn hoặc ngữ cảnh ngắn cho nhóm câu hỏi")
    String content;

    @JsonPropertyDescription("Các câu hỏi của đoạn văn này, ít nhất 1")
    List<GeneratedQuestion> questions;
}
