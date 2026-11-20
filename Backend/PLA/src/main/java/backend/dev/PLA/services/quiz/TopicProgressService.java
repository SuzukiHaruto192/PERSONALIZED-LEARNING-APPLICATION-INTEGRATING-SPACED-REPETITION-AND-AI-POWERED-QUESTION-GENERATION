package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.UserTopicProgress;
import backend.dev.PLA.entities.quiz.UserTopicProgressId;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import backend.dev.PLA.repositories.quiz.UserTopicProgressRepository;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cập nhật lịch ôn FSRS trong user_topic_progress sau khi nộp bài. Đơn vị lập lịch là cặp (skill_id, topic_id) của
 * quiz trong skill_topics: làm quiz Reading chỉ giãn lịch của cặp Reading – topic, không đụng tới Listening.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicProgressService {
    static final BigDecimal EASY_THRESHOLD = new BigDecimal("7.5");
    static final BigDecimal NORMAL_THRESHOLD = new BigDecimal("5.0");
    static final BigDecimal HARD_THRESHOLD = new BigDecimal("2.5");

    UserTopicProgressRepository userTopicProgressRepository;
    TopicRepository topicRepository;
    SpacedRepetitionService spacedRepetitionService;

    /**
     * Chạy trong transaction của lượt nộp bài (lỗi thì rollback cả lượt). Quiz dùng custom_topic (không có topic_id)
     * hoặc cặp (skill, topic) của quiz đã bị gỡ khỏi skill_topics thì không có lịch ôn, bỏ qua. Chỉ ghi DB, không trả
     * kết quả cho người dùng.
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAttempt(Long userId, Quiz quiz, int correctCount, int totalQuestions, Instant submittedAt) {
        if (quiz.getTopic() == null || quiz.getSkill() == null || totalQuestions <= 0) {
            return;
        }
        UserTopicProgressId id = new UserTopicProgressId(
                userId, quiz.getSkill().getId(), quiz.getTopic().getId());

        if (topicRepository.lockSkillTopic(id.getSkillId(), id.getTopicId()).isEmpty()) {
            return;
        }

        userTopicProgressRepository.insertIfAbsent(id.getUserId(), id.getSkillId(), id.getTopicId());
        UserTopicProgress progress = userTopicProgressRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        BigDecimal score = calculateScore(correctCount, totalQuestions);
        int rating = toRating(score);

        FSRSResult result = spacedRepetitionService.calculateNextReview(
                progress.getStability(), progress.getDifficulty(), progress.getLastReviewDate(), rating);
        FsrsState stateAfter = spacedRepetitionService.nextState(progress.getFsrsState(), rating);

        progress.setStability(result.getStability());
        progress.setDifficulty(result.getDifficulty());
        progress.setLastReviewDate(result.getLastReviewAt().toLocalDate());
        progress.setNextReviewDate(result.getNextReviewAt().toLocalDate());
        progress.setFsrsState(stateAfter);
        if (rating == 1) {
            progress.setLapses(progress.getLapses() + 1);
        }
        progress.setAttemptsCount(progress.getAttemptsCount() + 1);
        progress.setQuestionsAnswered(progress.getQuestionsAnswered() + totalQuestions);
        progress.setQuestionsCorrect(progress.getQuestionsCorrect() + correctCount);
        progress.setMastery(calculateMastery(progress.getQuestionsCorrect(), progress.getQuestionsAnswered()));
        progress.setLastPracticedAt(submittedAt);
        userTopicProgressRepository.saveAndFlush(progress);
    }

    /** Điểm thang 10 = đúng × 10 / tổng, làm tròn 1 chữ số thập phân (HALF_UP). Câu bỏ trống tính sai. */
    public static BigDecimal calculateScore(int correctCount, int totalQuestions) {
        return BigDecimal.valueOf(correctCount * 10L)
                .divide(BigDecimal.valueOf(totalQuestions), 1, RoundingMode.HALF_UP);
    }

    /** Điểm đã làm tròn → rating FSRS: ≥7.5 → 4, ≥5 → 3, ≥2.5 → 2, còn lại → 1. Bằng ngưỡng thì thuộc khoảng trên. */
    public static int toRating(BigDecimal score) {
        if (score.compareTo(EASY_THRESHOLD) >= 0) return 4;
        if (score.compareTo(NORMAL_THRESHOLD) >= 0) return 3;
        if (score.compareTo(HARD_THRESHOLD) >= 0) return 2;
        return 1;
    }

    /** mastery = tổng câu đúng / tổng câu đã làm × 100, làm tròn 2 chữ số thập phân. */
    static BigDecimal calculateMastery(int questionsCorrect, int questionsAnswered) {
        if (questionsAnswered <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(questionsCorrect * 100L)
                .divide(BigDecimal.valueOf(questionsAnswered), 2, RoundingMode.HALF_UP);
    }
}
