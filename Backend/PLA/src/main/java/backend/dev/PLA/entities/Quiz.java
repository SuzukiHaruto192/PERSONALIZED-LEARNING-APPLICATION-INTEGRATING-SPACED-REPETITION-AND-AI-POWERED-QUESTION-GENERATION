package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name= "name" , length = 255)
    private String nameQuiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_skill")
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_topic")
    private Topic topic;

    @ManyToMany(mappedBy = "quizzes")
    @Builder.Default
    private Set<Question> questions = new HashSet<>();

    @OneToMany(mappedBy = "quiz",cascade = CascadeType.ALL , fetch = FetchType.LAZY ,  orphanRemoval = true)
    private List<History>  histories ;
}
