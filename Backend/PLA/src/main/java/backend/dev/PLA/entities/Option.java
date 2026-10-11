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
@Table(name = "options")
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name = "is_correct")
    private boolean isCorrect ;

    @Column(name = "content")
    private String content ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_question")
    private Question question;

    @OneToMany(mappedBy = "option" , fetch = FetchType.LAZY ,  cascade = CascadeType.ALL ,  orphanRemoval = true)
    private List<HistoryDetail> historyDetails;
}
