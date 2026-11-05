package backend.dev.PLA.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptionRequest {
    @NotNull(message = "QUESTION_ID_BLANK")
    Long questionId;

    @NotNull(message = "POSITION_BLANK")
    Short position;

    @NotBlank(message = "OPTION_CONTENT_BLANK")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "OPTION_CONTENT_PATTERN")
    String content;

    @NotNull(message = "ANSWER_BLANK")
    Boolean isCorrect;
}
