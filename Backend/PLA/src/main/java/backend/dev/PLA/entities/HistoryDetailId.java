package backend.dev.PLA.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HistoryDetailId implements Serializable {

    @Column(name = "id_history")
    private Long idHistory;

    @Column(name = "id_question")
    private Long idQuestion;
}
