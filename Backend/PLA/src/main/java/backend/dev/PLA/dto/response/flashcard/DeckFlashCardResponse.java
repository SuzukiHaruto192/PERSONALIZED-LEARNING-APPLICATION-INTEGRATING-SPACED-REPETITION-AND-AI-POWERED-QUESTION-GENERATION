package backend.dev.PLA.dto.response.flashcard;

import backend.dev.PLA.enums.FsrsState;
import java.time.Instant;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một thẻ trong một deck, kèm lịch ôn FSRS của cặp (deck, thẻ). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeckFlashCardResponse {
    Long deckId;
    String deckName;
    Long flashCardId;
    String frontContent;
    String backContent;
    String example;
    Long sourcePassageId;
    FsrsState fsrsState;
    Integer lapses;
    LocalDate lastReviewDate;
    LocalDate nextReviewDate;
    // Số ngày đã quá hạn ôn (0 nếu chưa tới hạn hoặc thẻ mới)
    long overdueDays;
    Instant addedAt;
}
