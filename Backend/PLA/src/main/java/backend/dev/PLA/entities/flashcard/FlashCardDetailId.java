package backend.dev.PLA.entities.flashcard;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FlashCardDetailId implements Serializable {

    @Column(name = "deck_id")
    private Long deckId;

    @Column(name = "flashcard_id")
    private Long flashcardId;
}
