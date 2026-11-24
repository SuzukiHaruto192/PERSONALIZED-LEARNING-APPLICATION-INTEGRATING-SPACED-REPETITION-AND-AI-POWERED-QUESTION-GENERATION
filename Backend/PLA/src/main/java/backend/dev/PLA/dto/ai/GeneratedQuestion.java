package backend.dev.PLA.dto.ai;

import static backend.dev.PLA.services.ai.AiQuizRules.CORRECT_TEXT_MAX_LENGTH;
import static backend.dev.PLA.services.ai.AiQuizRules.MAX_OPTIONS;
import static backend.dev.PLA.services.ai.AiQuizRules.MIN_OPTIONS;

import backend.dev.PLA.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một câu hỏi do LLM sinh. Giới hạn trong description lấy từ AiQuizRules để luôn khớp với validator. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedQuestion {
    @JsonPropertyDescription("SINGLE_CHOICE hoặc FILL_BLANK")
    QuestionType type;

    @JsonPropertyDescription("Nội dung câu hỏi bằng tiếng Anh. FILL_BLANK đánh dấu chỗ trống bằng ___")
    String content;

    @JsonPropertyDescription("Chỉ dùng cho FILL_BLANK: đáp án đúng duy nhất, tối đa " + CORRECT_TEXT_MAX_LENGTH
            + " ký tự. SINGLE_CHOICE để null")
    String correctText;

    @JsonPropertyDescription("Giải thích ngắn bằng tiếng Việt")
    String explanation;

    @JsonPropertyDescription("Chỉ dùng cho SINGLE_CHOICE: " + MIN_OPTIONS + "-" + MAX_OPTIONS
            + " đáp án, đúng 1 đáp án isCorrect=true. FILL_BLANK để []")
    List<GeneratedOption> options;
}
