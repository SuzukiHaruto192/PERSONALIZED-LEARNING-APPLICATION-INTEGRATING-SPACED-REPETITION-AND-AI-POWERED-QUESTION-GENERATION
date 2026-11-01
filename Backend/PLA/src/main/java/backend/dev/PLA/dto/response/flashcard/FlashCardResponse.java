package backend.dev.PLA.dto.response.flashcard;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardResponse {
    Long id;
    String frontContent;
    String backContent;
}
