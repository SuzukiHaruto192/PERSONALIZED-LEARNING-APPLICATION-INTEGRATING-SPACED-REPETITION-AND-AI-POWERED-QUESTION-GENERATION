package backend.dev.PLA.dto.ai;

import backend.dev.PLA.enums.CefrLevel;
import backend.dev.PLA.enums.FsrsState;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Dữ liệu đưa vào prompt sinh quiz. Chỉ chứa giá trị mô tả, KHÔNG chứa id (userId, skillId, topicId, formatId...)
 * để id nội bộ không lộ ra LLM. Các field null thì không được in vào prompt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizPromptInput {
    // <quiz_spec>
    String skillName;
    String formatName;
    short difficulty;
    int questionCount;
    String topicName;
    CefrLevel topicCefrLevel;

    // <user_input> – đã sanitize
    String title;
    String description;
    String customTopic;

    // <learner_level> – từ bảng users
    BigDecimal currentScore;
    BigDecimal targetScore;

    // <learner_progress> – từ user_topic_progress; hasProgress = false nếu chưa luyện topic này
    boolean hasProgress;
    Integer attemptsCount;
    Integer questionsAnswered;
    Integer questionsCorrect;
    BigDecimal mastery;
    FsrsState fsrsState;
    Double stability;
    Double fsrsDifficulty;
    Integer lapses;
    LocalDate lastReviewDate;
    LocalDate nextReviewDate;

    // <output_rules>
    boolean needsPassage;
}
