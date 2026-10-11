package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "history")
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Quiz quiz;

    @Column(name = "number_of_correct_answers")
    private Integer numberOfCorrectAnswers;

    @OneToMany(mappedBy = "history" ,  fetch = FetchType.LAZY ,   cascade = CascadeType.ALL ,  orphanRemoval = true )
    private List<HistoryDetail> historyDetails;
}
