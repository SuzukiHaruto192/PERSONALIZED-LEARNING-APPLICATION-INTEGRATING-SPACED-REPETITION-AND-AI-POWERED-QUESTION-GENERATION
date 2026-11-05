package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.enums.FsrsState;
import backend.dev.PLA.services.repetition.SpacedRepetitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class SpacedRepetitionServiceTest {
    private static final double DELTA = 1e-6;

    private final SpacedRepetitionService service = new SpacedRepetitionService();

    @ParameterizedTest
    @CsvSource({
        "NEW, 1, LEARNING",
        "LEARNING, 1, LEARNING",
        "REVIEW, 1, RELEARNING",
        "RELEARNING, 1, RELEARNING",
        "NEW, 2, REVIEW",
        "NEW, 3, REVIEW",
        "NEW, 4, REVIEW",
        "LEARNING, 3, REVIEW",
        "RELEARNING, 3, REVIEW",
        "REVIEW, 3, REVIEW"
    })
    public void nextStateTest(FsrsState current, int rating, FsrsState expected) {
        assertEquals(expected, service.nextState(current, rating));
    }

    @ParameterizedTest
    @CsvSource({"1, 0.4872", "2, 1.4003", "3, 3.7145", "4, 13.8206"})
    public void initialStabilityTest(int rating, double expected) {
        assertEquals(expected, service.initialStability(rating), DELTA);
    }

    @ParameterizedTest
    @CsvSource({"1, 7.6214", "2, 6.3916", "3, 5.1618", "4, 3.9320"})
    public void initialDifficultyTest(int rating, double expected) {
        assertEquals(expected, service.initialDifficulty(rating), DELTA);
    }

    @Test
    public void retrievabilityTest_notElapsedIsFullyRemembered() {
        assertEquals(1.0, service.retrievability(0, 10.0), DELTA);
        assertEquals(1.0, service.retrievability(-1, 10.0), DELTA);
    }

    @Test
    public void retrievabilityTest_nonPositiveStabilityIsForgotten() {
        assertEquals(0.0, service.retrievability(5, 0.0), DELTA);
        assertEquals(0.0, service.retrievability(5, -1.0), DELTA);
    }

    @Test
    public void retrievabilityTest_equalsTargetRetentionWhenElapsedEqualsStability() {
        // Theo định nghĩa của FSRS: sau S ngày, khả năng nhớ đúng bằng 90%
        assertEquals(0.9, service.retrievability(10, 10.0), DELTA);
    }

    @Test
    public void retrievabilityTest_decreasesOverTime() {
        assertTrue(service.retrievability(20, 10.0) < service.retrievability(5, 10.0));
    }

    @ParameterizedTest
    @CsvSource({"1, 6.744371", "2, 5.874693", "3, 5.005016", "4, 4.135338"})
    public void nextDifficultyTest(int rating, double expected) {
        assertEquals(expected, service.nextDifficulty(5.0, rating), DELTA);
    }

    @Test
    public void nextDifficultyTest_clampedToMaximum() {
        assertEquals(10.0, service.nextDifficulty(10.0, 1), DELTA);
    }

    @Test
    public void nextDifficultyTest_clampedToMinimum() {
        assertEquals(1.0, service.nextDifficulty(1.0, 4), DELTA);
    }

    @ParameterizedTest
    @CsvSource({"2, 15.699061", "3, 35.083894", "4, 82.128738"})
    public void nextStabilityTest_afterSuccess(int rating, double expected) {
        assertEquals(expected, service.nextStability(5.0, 10.0, 0.9, rating), DELTA);
    }

    @Test
    public void nextStabilityTest_afterFailure() {
        assertEquals(2.560383, service.nextStability(5.0, 10.0, 0.9, 1), DELTA);
    }

    @Test
    public void nextStabilityTest_afterFailureNeverExceedsCurrentStability() {
        assertEquals(0.5, service.nextStability(5.0, 0.5, 0.1, 1), DELTA);
    }

    @Test
    public void nextStabilityTest_afterFailureHasMinimum() {
        assertEquals(0.1, service.nextStability(5.0, 0.01, 0.9, 1), DELTA);
    }

    @Test
    public void nextStabilityTest_successWithFullRetrievabilityKeepsStability() {
        assertEquals(10.0, service.nextStability(5.0, 10.0, 1.0, 3), DELTA);
    }

    @ParameterizedTest
    @CsvSource({"0.0, 1", "-5.0, 1", "0.4, 1", "1.4, 1", "3.7145, 4", "13.8206, 14", "100.0, 100"})
    public void calculateIntervalTest(double stability, int expected) {
        assertEquals(expected, service.calculateInterval(stability));
    }

    @ParameterizedTest
    @CsvSource({"1, 0.4872, 7.6214, 1", "2, 1.4003, 6.3916, 1", "3, 3.7145, 5.1618, 4", "4, 13.8206, 3.9320, 14"})
    public void calculateNextReviewTest_firstReview(int rating, double stability, double difficulty, int interval) {
        LocalDateTime before = LocalDateTime.now();

        FSRSResult result = service.calculateNextReview(0.0, 0.0, (LocalDateTime) null, rating);

        LocalDateTime after = LocalDateTime.now();
        assertEquals(stability, result.getStability(), DELTA);
        assertEquals(difficulty, result.getDifficulty(), DELTA);
        assertEquals(interval, result.getIntervalDays());
        assertTrue(!result.getLastReviewAt().isBefore(before)
                && !result.getLastReviewAt().isAfter(after));
        assertEquals(result.getLastReviewAt().plusDays(interval), result.getNextReviewAt());
    }

    static Stream<Arguments> incompleteState() {
        LocalDateTime lastReview = LocalDateTime.now().minusDays(3);
        return Stream.of(
                Arguments.of(null, 5.0, lastReview),
                Arguments.of(0.0, 5.0, lastReview),
                Arguments.of(-1.0, 5.0, lastReview),
                Arguments.of(10.0, null, lastReview),
                Arguments.of(10.0, 0.0, lastReview),
                Arguments.of(10.0, -1.0, lastReview),
                Arguments.of(10.0, 5.0, null));
    }

    @ParameterizedTest
    @MethodSource("incompleteState")
    public void calculateNextReviewTest_incompleteStateIsTreatedAsFirstReview(
            Double stability, Double difficulty, LocalDateTime lastReview) {
        FSRSResult result = service.calculateNextReview(stability, difficulty, lastReview, 3);

        assertEquals(3.7145, result.getStability(), DELTA);
        assertEquals(5.1618, result.getDifficulty(), DELTA);
        assertEquals(4, result.getIntervalDays());
    }

    @ParameterizedTest
    @CsvSource({"1", "2", "3", "4"})
    public void calculateNextReviewTest_repeatReview(int rating) {
        LocalDateTime lastReview = LocalDateTime.now().minusDays(10);

        FSRSResult result = service.calculateNextReview(10.0, 5.0, lastReview, rating);

        // Tính số ngày theo mốc mà service đã dùng, để test không lệch khi chạy qua nửa đêm
        long elapsedDays = ChronoUnit.DAYS.between(
                lastReview.toLocalDate(), result.getLastReviewAt().toLocalDate());
        double expectedStability = service.nextStability(5.0, 10.0, service.retrievability(elapsedDays, 10.0), rating);
        double expectedDifficulty = service.nextDifficulty(5.0, rating);
        int expectedInterval = service.calculateInterval(expectedStability);

        assertEquals(roundTo4Digits(expectedStability), result.getStability(), DELTA);
        assertEquals(roundTo4Digits(expectedDifficulty), result.getDifficulty(), DELTA);
        assertEquals(expectedInterval, result.getIntervalDays());
        assertEquals(result.getLastReviewAt().plusDays(expectedInterval), result.getNextReviewAt());
    }

    @Test
    public void calculateNextReviewTest_betterRatingGivesLongerInterval() {
        LocalDateTime lastReview = LocalDateTime.now().minusDays(10);

        int again = service.calculateNextReview(10.0, 5.0, lastReview, 1).getIntervalDays();
        int hard = service.calculateNextReview(10.0, 5.0, lastReview, 2).getIntervalDays();
        int good = service.calculateNextReview(10.0, 5.0, lastReview, 3).getIntervalDays();
        int easy = service.calculateNextReview(10.0, 5.0, lastReview, 4).getIntervalDays();

        assertTrue(again < hard && hard < good && good < easy);
    }

    @Test
    public void calculateNextReviewTest_lastReviewInFutureIsTreatedAsNoElapsedTime() {
        LocalDateTime lastReview = LocalDateTime.now().plusDays(5);

        FSRSResult result = service.calculateNextReview(10.0, 5.0, lastReview, 3);

        assertEquals(10.0, result.getStability(), DELTA);
        assertEquals(10, result.getIntervalDays());
    }

    @Test
    public void calculateNextReviewTest_resultIsRoundedToFourDecimals() {
        FSRSResult result =
                service.calculateNextReview(10.0, 5.0, LocalDateTime.now().minusDays(10), 3);

        assertEquals(roundTo4Digits(result.getStability()), result.getStability());
        assertEquals(roundTo4Digits(result.getDifficulty()), result.getDifficulty());
    }

    @Test
    public void calculateNextReviewTest_localDateOverloadUsesStartOfDay() {
        LocalDate lastReviewDate = LocalDate.now().minusDays(10);

        FSRSResult fromDate = service.calculateNextReview(10.0, 5.0, lastReviewDate, 3);
        FSRSResult fromDateTime = service.calculateNextReview(10.0, 5.0, lastReviewDate.atStartOfDay(), 3);

        assertEquals(fromDateTime.getStability(), fromDate.getStability(), DELTA);
        assertEquals(fromDateTime.getDifficulty(), fromDate.getDifficulty(), DELTA);
        assertEquals(fromDateTime.getIntervalDays(), fromDate.getIntervalDays());
    }

    @Test
    public void calculateNextReviewTest_localDateOverloadWithNullIsFirstReview() {
        FSRSResult result = service.calculateNextReview(10.0, 5.0, (LocalDate) null, 3);

        assertEquals(3.7145, result.getStability(), DELTA);
        assertEquals(5.1618, result.getDifficulty(), DELTA);
        assertEquals(4, result.getIntervalDays());
    }

    private double roundTo4Digits(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
