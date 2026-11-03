package backend.dev.PLA.entities;

import backend.dev.PLA.enums.FsrsState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "flashcard_detail")
public class FlashCardDetail {
    @EmbeddedId
    @Builder.Default
    private FlashCardDetailId id = new FlashCardDetailId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("deckId")
    @JoinColumn(name = "deck_id")
    private FlashCardDeck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("flashcardId")
    @JoinColumn(name = "flashcard_id")
    private FlashCard flashCard;

    @Column(name = "difficulty")
    private Double difficulty;

    @Column(name = "stability")
    private Double stability;

    @Column(name = "lapses", nullable = false)
    @Builder.Default
    private Integer lapses = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "fsrs_state", length = 20, nullable = false)
    @Builder.Default
    private FsrsState fsrsState = FsrsState.NEW;

    @Column(name = "last_review")
    private LocalDate lastReviewDate;

    @Column(name = "next_review")
    private LocalDate nextReviewDate;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;
}
