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
@Table(name = "topics")
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name" ,  length = 50)
    private String topicName;

    @OneToMany(mappedBy = "topic" ,  fetch = FetchType.LAZY ,  cascade = CascadeType.ALL ,   orphanRemoval = true)
    private List<FlashCard> flashCards;

    @OneToMany(mappedBy = "topic" , fetch = FetchType.LAZY , cascade = CascadeType.ALL ,   orphanRemoval = true)
    private List<Quiz> quizzes;
}
