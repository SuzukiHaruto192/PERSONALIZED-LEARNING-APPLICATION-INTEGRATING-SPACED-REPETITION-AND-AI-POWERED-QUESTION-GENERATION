package backend.dev.PLA.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptionRequest {
    @NotBlank(message =  "QUESTION_ID_BLANK")
    Long idQuestion ;

    @NotBlank(message = "OPTION_CONTENT_BLANK")
    @Pattern(
            regexp = "^[a-zA-Z0-9 ]+$",
            message = "OPTION_CONTENT_PATTERN"
    )
    String content ;

    @NotBlank(message = "ANSWER_BLANK")
    Boolean isAnswer ;
}
