package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "flashcard")
public class FlashCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "front_content", length = 255, nullable = false)
    private String frontContent;

    @Column(name = "back_content", length = 255, nullable = false)
    private String backContent;

    @Column(name = "example", columnDefinition = "TEXT")
    private String example;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_quiz_id")
    private Quiz sourceQuiz;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "flashCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<FlashCardDetail> flashCardDetails;
}
