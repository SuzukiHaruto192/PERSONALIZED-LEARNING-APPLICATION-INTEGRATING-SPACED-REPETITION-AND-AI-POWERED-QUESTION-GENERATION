package backend.dev.PLA.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE )
public class QuestionRequest {
    @NotBlank(message = "QUESTION_CONTENT_BLANK")
    @Pattern(
            regexp = "^[a-zA-Z0-9 ]+$",
            message = "QUESTION_CONTENT_PATTERN"
    )
    String content;


    String resource;
}
