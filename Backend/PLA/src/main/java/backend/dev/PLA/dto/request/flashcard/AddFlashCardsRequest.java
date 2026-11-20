package backend.dev.PLA.dto.request.flashcard;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Body của POST /api/v1/decks/{deckId}/flashcards: thêm các thẻ có sẵn vào deck. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddFlashCardsRequest {
    @NotEmpty(message = "FLASHCARD_IDS_EMPTY")
    @Size(max = 100, message = "FLASHCARD_IDS_TOO_MANY")
    List<@NotNull(message = "FLASHCARD_ID_REQUIRED") Long> flashCardIds;
}
