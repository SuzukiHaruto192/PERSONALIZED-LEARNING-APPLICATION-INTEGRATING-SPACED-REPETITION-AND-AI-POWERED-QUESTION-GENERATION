package backend.dev.PLA.dto.request.question;

import backend.dev.PLA.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionRequest {
    @NotNull(message = "QUIZ_ID_BLANK")
    Long quizId;

    @NotNull(message = "POSITION_BLANK")
    Short position;

    // NULL thì lấy mặc định SINGLE_CHOICE
    QuestionType type;

    @NotBlank(message = "QUESTION_CONTENT_BLANK")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "QUESTION_CONTENT_PATTERN")
    String content;

    @Size(max = 255, message = "CORRECT_TEXT_SIZE")
    String correctText;

    String explanation;

    Long grammarTopicId;
}
