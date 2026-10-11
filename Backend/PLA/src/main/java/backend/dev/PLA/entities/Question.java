package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content" , length = 255)
    private String content;

    @Column(name="resource" , length = 255)
    private String resource ;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "quiz_detail" ,
            joinColumns = @JoinColumn(name = "id_question") ,
            inverseJoinColumns = @JoinColumn(name = "id_quiz")
    )
    @Builder.Default
    private Set<Quiz> quizzes = new HashSet<>();

    @OneToMany(mappedBy = "question",fetch = FetchType.LAZY ,  cascade = CascadeType.ALL ,  orphanRemoval = true )
    private List<Option> options ;

    @OneToMany(mappedBy = "question" , fetch = FetchType.LAZY ,  cascade = CascadeType.ALL ,  orphanRemoval = true )
    private List<HistoryDetail> historyDetails;
}
