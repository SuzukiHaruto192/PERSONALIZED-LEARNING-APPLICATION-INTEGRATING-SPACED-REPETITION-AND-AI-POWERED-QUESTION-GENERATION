package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "flashcard")
public class FlashCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "front_content" , length = 255 , nullable = false)
    private String frontContent;

    @Column(name = "back_content"  , length = 255 , nullable = false)
    private String backContent;

    @OneToMany(mappedBy = "flashCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY ,  orphanRemoval = true)
    private List<FlashCardDetail> flashCardDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_topic")
    private Topic topic;
}
