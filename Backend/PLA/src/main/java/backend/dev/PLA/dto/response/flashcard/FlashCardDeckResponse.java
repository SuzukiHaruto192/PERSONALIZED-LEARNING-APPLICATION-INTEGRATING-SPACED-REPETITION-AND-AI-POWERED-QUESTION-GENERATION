package backend.dev.PLA.dto.response.flashcard;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardDeckResponse {
    Long id;
    String name;
    String description;
    Instant createdAt;
}
