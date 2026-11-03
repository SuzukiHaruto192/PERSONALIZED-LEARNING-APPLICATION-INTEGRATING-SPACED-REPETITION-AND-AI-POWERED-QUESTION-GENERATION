package backend.dev.PLA.dto.request.flashcard;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewDeckRequest {
    @NotBlank(message = "DECK_NAME_BLANK")
    @Size(max = 100, message = "DECK_NAME_SIZE")
    String name;

    @Size(max = 255, message = "DECK_DESCRIPTION_SIZE")
    String description;
}
