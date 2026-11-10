package backend.dev.PLA.entities.quiz;

import jakarta.persistence.*;
import lombok.*;

// Khung tối thiểu cho Task 1 (xoá quiz). API CRUD passage làm ở Task 2.
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "passages")
public class Passage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "position", nullable = false)
    private Short position;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;
}
