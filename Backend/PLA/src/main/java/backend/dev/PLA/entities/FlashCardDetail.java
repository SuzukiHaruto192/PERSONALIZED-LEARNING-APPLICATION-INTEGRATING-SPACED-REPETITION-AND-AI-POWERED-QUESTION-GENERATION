package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name= "flashcard_detail")
public class FlashCardDetail {
    @EmbeddedId
    @Builder.Default
    private FlashCardDetailId id = new FlashCardDetailId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idFlashcard")
    @JoinColumn(name = "id_flashcard")
    private FlashCard flashCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUser")
    @JoinColumn(name = "id_user")
    private User user;

    @Column(name = "difficulty" )
    private Double difficulty;

    @Column(name = "stability")
    private Double stability;

    @Column(name = "lapses")
    private Integer lapses ;

    @Column(name = "last_review")
    private LocalDateTime lastReviewDate;

    @Column(name = "next_review")
    private LocalDateTime nextReviewDate;

    @Column(name = "state")
    private boolean state;
}
