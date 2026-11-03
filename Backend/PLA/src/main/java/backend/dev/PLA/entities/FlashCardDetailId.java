package backend.dev.PLA.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

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
