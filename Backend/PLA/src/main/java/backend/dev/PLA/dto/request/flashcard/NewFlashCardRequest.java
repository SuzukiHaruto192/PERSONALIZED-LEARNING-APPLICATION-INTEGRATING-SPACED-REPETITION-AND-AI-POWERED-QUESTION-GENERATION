package backend.dev.PLA.dto.request.flashcard;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE )
public class NewFlashCardRequest {
    @NotBlank(message = "FRONT_CONTENT_BLANK")
    String frontContent;

    @NotBlank(message = "BACK_CONTENT_BLANK")
    String backContent;

}
