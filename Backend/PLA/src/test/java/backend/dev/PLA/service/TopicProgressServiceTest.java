package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.UserTopicProgress;
import backend.dev.PLA.entities.quiz.UserTopicProgressId;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import backend.dev.PLA.repositories.quiz.UserTopicProgressRepository;
import backend.dev.PLA.services.quiz.TopicProgressService;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicProgressServiceTest {
    static final long USER_ID = 7L;
    static final short READING = 2;
    static final long TOPIC_ID = 12L;
    static final Instant SUBMITTED_AT = Instant.parse("2026-11-19T08:00:00Z");
    static final UserTopicProgressId KEY = new UserTopicProgressId(USER_ID, READING, TOPIC_ID);

    @Mock
    UserTopicProgressRepository repository;

    @Mock
    TopicRepository topicRepository;

    // Dùng thuật toán thật để kiểm tra đúng cột được ghi từ FSRSResult
    final SpacedRepetitionService fsrs = new SpacedRepetitionService();

    TopicProgressService service;
    Quiz quiz;

    @BeforeEach
    void init() {
        service = new TopicProgressService(repository, topicRepository, fsrs);
        quiz = Quiz.builder()
                .id(1L)
                .skill(Skill.builder().id(READING).build())
                .topic(Topic.builder().id(TOPIC_ID).build())
                .build();
    }

    // ===================== Quy đổi điểm → rating =====================

    @ParameterizedTest(name = "{0}/{1} → {2} → rating {3}")
    @CsvSource({
        "10, 10, 10.0, 4",
        "3, 4, 7.5, 4",
        "149, 200, 7.5, 4", // 7.45 làm tròn lên 7.5
        "22, 30, 7.3, 3",
        "7, 10, 7.0, 3",
        "1, 2, 5.0, 3",
        "3, 10, 3.0, 2",
        "1, 4, 2.5, 2",
        "49, 200, 2.5, 2", // 2.45 làm tròn lên 2.5
        "2, 10, 2.0, 1",
        "0, 10, 0.0, 1"
    })
    void scoreAndRating(int correct, int total, String expectedScore, int expectedRating) {
        BigDecimal score = TopicProgressService.calculateScore(correct, total);
        assertEquals(new BigDecimal(expectedScore), score);
        assertEquals(expectedRating, TopicProgressService.toRating(score));
    }

    // ===================== recordAttempt =====================

    @Test
    void customTopicQuiz_skipsUpdate() {
        quiz.setTopic(null);

        service.recordAttempt(USER_ID, quiz, 8, 10, SUBMITTED_AT);
        verifyNoInteractions(repository, topicRepository);
    }

    @Test
    void skillTopicPairRemoved_skipsUpdate() {
        // Admin đã gỡ skill khỏi topic sau khi quiz được tạo: không ghi progress (tránh lỗi FK)
        when(topicRepository.lockSkillTopic(READING, TOPIC_ID)).thenReturn(Optional.empty());

        service.recordAttempt(USER_ID, quiz, 8, 10, SUBMITTED_AT);
        verifyNoInteractions(repository);
    }

    @Test
    void firstAttempt_createsRowAndUsesInitialStability() {
        UserTopicProgress progress = newProgress();
        givenPairExists();
        when(repository.findByIdForUpdate(KEY)).thenReturn(Optional.of(progress));

        service.recordAttempt(USER_ID, quiz, 8, 10, SUBMITTED_AT);

        // Tạo dòng nếu chưa có rồi mới đọc có khoá, cùng khoá (user nộp, skill/topic của quiz)
        var order = inOrder(topicRepository, repository);
        order.verify(topicRepository).lockSkillTopic(READING, TOPIC_ID);
        order.verify(repository).insertIfAbsent(USER_ID, READING, TOPIC_ID);
        order.verify(repository).findByIdForUpdate(KEY);
        order.verify(repository).saveAndFlush(progress);

        LocalDate today = LocalDate.now();
        // 8/10 = 8.0 → rating 4 → S0 = w[3], D0 = w[4] - w[5], interval 14 ngày; NEW → REVIEW
        assertEquals(13.8206, progress.getStability());
        assertEquals(3.932, progress.getDifficulty());
        assertEquals(today, progress.getLastReviewDate());
        assertEquals(today.plusDays(14), progress.getNextReviewDate());
        assertEquals(FsrsState.REVIEW, progress.getFsrsState());
        assertEquals(1, progress.getAttemptsCount());
        assertEquals(10, progress.getQuestionsAnswered());
        assertEquals(8, progress.getQuestionsCorrect());
        assertEquals(SUBMITTED_AT, progress.getLastPracticedAt());
        assertEquals(0, progress.getLapses());
        assertEquals(new BigDecimal("80.00"), progress.getMastery());
    }

    @Test
    void firstAttemptFailed_goesToLearningAndCountsLapse() {
        UserTopicProgress progress = newProgress();
        givenPairExists();
        when(repository.findByIdForUpdate(KEY)).thenReturn(Optional.of(progress));

        service.recordAttempt(USER_ID, quiz, 1, 10, SUBMITTED_AT);

        // 1/10 = 1.0 → rating 1: NEW → LEARNING, S0 = w[0]
        assertEquals(FsrsState.LEARNING, progress.getFsrsState());
        assertEquals(1, progress.getLapses());
        assertEquals(0.4872, progress.getStability());
    }

    @Test
    void secondAttempt_accumulatesAndRelearnsOnFail() {
        UserTopicProgress progress = newProgress();
        progress.setFsrsState(FsrsState.REVIEW);
        progress.setStability(13.8206);
        progress.setDifficulty(3.932);
        progress.setLastReviewDate(LocalDate.now().minusDays(3));
        progress.setNextReviewDate(LocalDate.now().plusDays(11));
        progress.setAttemptsCount(1);
        progress.setQuestionsAnswered(10);
        progress.setQuestionsCorrect(8);
        progress.setLapses(2);
        givenPairExists();
        when(repository.findByIdForUpdate(KEY)).thenReturn(Optional.of(progress));

        service.recordAttempt(USER_ID, quiz, 0, 5, SUBMITTED_AT);

        // 0/5 → rating 1: REVIEW → RELEARNING
        assertEquals(FsrsState.RELEARNING, progress.getFsrsState());
        assertEquals(3, progress.getLapses());
        assertEquals(2, progress.getAttemptsCount());
        assertEquals(15, progress.getQuestionsAnswered());
        assertEquals(8, progress.getQuestionsCorrect());
        // 8 / 15 × 100 = 53.333 → 53.33
        assertEquals(new BigDecimal("53.33"), progress.getMastery());
        // Quên thì stability không tăng so với trước
        assertEquals(true, progress.getStability() <= 13.8206);
        assertEquals(LocalDate.now(), progress.getLastReviewDate());
    }

    @Test
    void rowMissingAfterInsert_throwsUncategorized() {
        givenPairExists();
        when(repository.findByIdForUpdate(KEY)).thenReturn(Optional.empty());

        AppException ex =
                assertThrows(AppException.class, () -> service.recordAttempt(USER_ID, quiz, 5, 10, SUBMITTED_AT));
        assertSame(ErrorCode.UNCATEGORIZED_EXCEPTION, ex.getErrorCode());
        verify(repository, never()).saveAndFlush(any());
    }

    void givenPairExists() {
        when(topicRepository.lockSkillTopic(READING, TOPIC_ID)).thenReturn(Optional.of(1));
    }

    UserTopicProgress newProgress() {
        return UserTopicProgress.builder().id(KEY).build();
    }
}
