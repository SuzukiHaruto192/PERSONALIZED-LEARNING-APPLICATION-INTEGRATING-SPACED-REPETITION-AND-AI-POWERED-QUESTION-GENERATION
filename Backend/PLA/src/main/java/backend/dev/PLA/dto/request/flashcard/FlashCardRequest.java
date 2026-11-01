package backend.dev.PLA.dto.request.flashcard;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardRequest {

    @NotNull(message = "FLASHCARD_ID_REQUIRED")
    Long flashCardId;

    @NotNull(message = "RATING_REQUIRED")
    @Min(value = 1, message = "RATING_MIN")
    @Max(value = 4, message = "RATING_MAX")
    Integer rating;
}