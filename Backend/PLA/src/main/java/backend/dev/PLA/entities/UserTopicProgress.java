package backend.dev.PLA.entities;

import backend.dev.PLA.enums.FsrsState;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_topic_progress")
public class UserTopicProgress {
    @EmbeddedId
    @Builder.Default
    private UserTopicProgressId id = new UserTopicProgressId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "attempts_count", nullable = false)
    @Builder.Default
    private Integer attemptsCount = 0;

    @Column(name = "questions_answered", nullable = false)
    @Builder.Default
    private Integer questionsAnswered = 0;

    @Column(name = "questions_correct", nullable = false)
    @Builder.Default
    private Integer questionsCorrect = 0;

    @Column(name = "mastery", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal mastery = BigDecimal.ZERO;

    @Column(name = "last_practiced_at")
    private Instant lastPracticedAt;

    @Column(name = "difficulty")
    private Double difficulty;

    @Column(name = "stability")
    private Double stability;

    @Column(name = "lapses", nullable = false)
    @Builder.Default
    private Integer lapses = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "fsrs_state", length = 20, nullable = false)
    @Builder.Default
    private FsrsState fsrsState = FsrsState.NEW;

    @Column(name = "last_review")
    private LocalDate lastReviewDate;

    @Column(name = "next_review")
    private LocalDate nextReviewDate;
}
