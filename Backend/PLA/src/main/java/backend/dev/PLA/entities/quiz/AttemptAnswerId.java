package backend.dev.PLA.entities.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AttemptAnswerId implements Serializable {

    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "question_id")
    private Long questionId;
}
