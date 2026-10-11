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
@Table(name = "skills")
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name ="name", length = 50)
    private String skillName;

    @OneToMany( mappedBy = "skill" , cascade = CascadeType.ALL ,  fetch = FetchType.LAZY , orphanRemoval = true)
    private List<Quiz> quizzes;

}
