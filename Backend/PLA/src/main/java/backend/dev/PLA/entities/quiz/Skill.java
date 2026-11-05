package backend.dev.PLA.entities.quiz;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "skills")
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "icon", length = 50)
    private String icon;

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    private List<Quiz> quizzes;

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    private List<QuizFormat> quizFormats;
}
