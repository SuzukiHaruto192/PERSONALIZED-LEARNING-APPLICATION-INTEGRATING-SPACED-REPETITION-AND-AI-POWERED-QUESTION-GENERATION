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

    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "id_flashcard")
    private Long idFlashcard;
}