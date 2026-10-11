package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "history_detail")
public class HistoryDetail {
    @EmbeddedId
    @Builder.Default
    private HistoryDetailId id = new HistoryDetailId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idQuestion")
    @JoinColumn(name = "id_question")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idHistory")
    @JoinColumn(name = "id_history")
    private History history;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chose_option")
    private Option option;

    @Column(name = "difficulty" )
    private Double difficulty;

    @Column(name = "stability")
    private Double stability;

    @Column(name = "lapses")
    private Integer lapses ;

    @Column(name = "last_review")
    private LocalDate lastReviewDate;

    @Column(name = "next_review")
    private LocalDate nextReviewDate;

    @Column(name = "state")
    private boolean state;

}
