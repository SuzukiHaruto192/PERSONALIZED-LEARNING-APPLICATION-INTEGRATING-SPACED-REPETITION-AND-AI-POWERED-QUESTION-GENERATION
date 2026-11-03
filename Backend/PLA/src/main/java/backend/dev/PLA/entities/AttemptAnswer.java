package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attempt_answers")
public class AttemptAnswer {
    @EmbeddedId
    @Builder.Default
    private AttemptAnswerId id = new AttemptAnswerId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private Option selectedOption;

    @Column(name = "answer_text", length = 255)
    private String answerText;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;
}
