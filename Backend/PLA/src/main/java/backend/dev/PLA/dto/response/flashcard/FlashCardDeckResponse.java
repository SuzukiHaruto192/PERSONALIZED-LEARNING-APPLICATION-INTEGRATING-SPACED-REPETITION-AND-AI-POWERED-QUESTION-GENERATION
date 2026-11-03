package backend.dev.PLA.dto.response.flashcard;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

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
