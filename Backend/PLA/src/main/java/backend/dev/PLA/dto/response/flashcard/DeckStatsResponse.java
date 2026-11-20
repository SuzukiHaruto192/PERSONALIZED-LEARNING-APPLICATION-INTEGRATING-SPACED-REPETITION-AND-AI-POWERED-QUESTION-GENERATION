package backend.dev.PLA.dto.response.flashcard;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeckStatsResponse {
    long totalCards;
    long dueToday;
    long newCards;
    long learning;
    long review;
    long relearning;
}
