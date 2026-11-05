package backend.dev.PLA.entities.quiz;

import backend.dev.PLA.enums.TopicGroupType;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "topic_groups",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_topic_groups_name_type",
                        columnNames = {"name", "type"}))
public class TopicGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TopicGroupType type;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Topic> topics;
}
