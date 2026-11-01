package backend.dev.PLA.services;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SpacedRepetitionService {
    // 17 trọng số mặc định chuẩn của thuật toán FSRS v4.5
    private static final double[] W = {
            0.4872, 1.4003, 3.7145, 13.8206, // w[0..3]: Stability khởi tạo cho AGAIN, HARD, NORMAL, EASY
            5.1618, 1.2298, 0.8975, 0.031,   // w[4..7]: Tham số tính Difficulty (Độ khó)
            1.6474, 0.1367, 1.0461,          // w[8..10]: Tham số tăng Stability khi nhớ (Recall)
            2.1072, 0.0793, 0.3246, 1.587,   // w[11..14]: Tham số tính Stability khi quên (Forget)
            0.2272, 2.8755                   // w[15..16]: Hệ số phạt (Hard penalty) và thưởng (Easy bonus)
    };

    private static final double REQUEST_RETENTION = 0.9; // Tỷ lệ nhớ mục tiêu (90%)
    private static final double DECAY = -0.5;
    private static final double FACTOR = 19.0 / 81.0; // Công thức: 0.9^(1/DECAY) - 1

    private static final double MIN_DIFFICULTY = 1.0;
    private static final double MAX_DIFFICULTY = 10.0;
    private static final double MIN_STABILITY = 0.1;

    /**
     * Tính toán Stability ban đầu dựa trên rating đầu tiên (1: Again, 2: Hard, 3: Normal/Good, 4: Easy)
     * S_0(G) = w[G - 1]
     */
    public double initialStability(int rating) {
        return W[rating - 1];
    }

    /**
     * Tính toán Difficulty ban đầu dựa trên rating đầu tiên
     * D_0(G) = clamp(w[4] - (G - 3) * w[5], 1, 10)
     */
    public double initialDifficulty(int rating) {
        double d = W[4] - (rating - 3) * W[5];
        return clamp(d, MIN_DIFFICULTY, MAX_DIFFICULTY);
    }

    /**
     * Tính Retrievability (khả năng nhớ lại thẻ sau elapsedDays ngày)
     * R(t, S) = (1 + FACTOR * (t / S))^DECAY
     */
    public double retrievability(double elapsedDays, double stability) {
        if (elapsedDays <= 0) {
            return 1.0;
        }
        if (stability <= 0) {
            return 0.0;
        }
        return Math.pow(1.0 + FACTOR * (elapsedDays / stability), DECAY);
    }

    /**
     * Tính Difficulty kế tiếp cho lần ôn tập tiếp theo
     * Delta D = -w[6] * (G - 3)
     * Mean Reversion về D_0(3): w[7] * D_0(3) + (1 - w[7]) * (D + Delta D)
     */
    public double nextDifficulty(double currentDifficulty, int rating) {
        double deltaD = -W[6] * (rating - 3);
        double newD = currentDifficulty + deltaD;
        double initD3 = initialDifficulty(3);
        double revertedD = W[7] * initD3 + (1.0 - W[7]) * newD;
        return clamp(revertedD, MIN_DIFFICULTY, MAX_DIFFICULTY);
    }

    /**
     * Tính Stability kế tiếp cho lần ôn tập tiếp theo
     * Rating 1 (Again): stability_after_failure (quên thẻ)
     * Rating 2..4 (Hard, Normal, Easy): stability_after_success (nhớ được thẻ)
     */
    public double nextStability(double currentDifficulty, double currentStability, double retrievability, int rating) {


        if (rating == 1) {
            double newS = W[11]
                    * Math.pow(currentDifficulty, -W[12])
                    * (Math.pow(currentStability + 1.0, W[13]) - 1.0)
                    * Math.exp(W[14] * (1.0 - retrievability));

            return Math.max(MIN_STABILITY, Math.min(newS, currentStability));
        } else {
            double hardPenalty = (rating == 2) ? W[15] : 1.0;
            double easyBonus = (rating == 4) ? W[16] : 1.0;

            double newS = currentStability * (1.0
                    + Math.exp(W[8])
                    * (11.0 - currentDifficulty)
                    * Math.pow(currentStability, -W[9])
                    * (Math.exp(W[10] * (1.0 - retrievability)) - 1.0)
                    * hardPenalty
                    * easyBonus);

            return Math.max(MIN_STABILITY, newS);
        }
    }

    /**
     * Tính số ngày khoảng cách ôn tập tiếp theo dựa trên Stability và target retention
     * Interval = (S / FACTOR) * (REQUEST_RETENTION^(1/DECAY) - 1)
     */
    public int calculateInterval(double stability) {
        if (stability <= 0) {
            return 1;
        }
        double interval = (stability / FACTOR) * (Math.pow(REQUEST_RETENTION, 1.0 / DECAY) - 1.0);
        return Math.max(1, (int) Math.round(interval));
    }

    /**
     * Tính toán FSRSResult cho thẻ flashcard dựa trên rating và thời gian ôn tập trước đó (LocalDateTime)
     */
    public FSRSResult calculateNextReview(Double currentStability, Double currentDifficulty, LocalDateTime lastReviewAt, int rating) {
        LocalDateTime now = LocalDateTime.now();

        // Nếu thẻ mới học lần đầu (chưa có review trước đó hoặc stability/difficulty <= 0)
        if (lastReviewAt == null || currentStability == null || currentStability <= 0
                || currentDifficulty == null || currentDifficulty <= 0) {
            double s0 = initialStability(rating);
            double d0 = initialDifficulty(rating);
            int intervalDays = calculateInterval(s0);

            return FSRSResult.builder()
                    .stability(round(s0))
                    .difficulty(round(d0))
                    .intervalDays(intervalDays)
                    .lastReviewAt(now)
                    .nextReviewAt(now.plusDays(intervalDays))
                    .build();
        }

        // Tính số ngày lịch đã trôi qua kể từ lần ôn tập trước
        long elapsedDays = ChronoUnit.DAYS.between(lastReviewAt.toLocalDate(), now.toLocalDate());
        if (elapsedDays < 0) {
            elapsedDays = 0;
        }

        double r = retrievability((double) elapsedDays, currentStability);
        double nextD = nextDifficulty(currentDifficulty, rating);
        double nextS = nextStability(currentDifficulty, currentStability, r, rating);
        int intervalDays = calculateInterval(nextS);

        return FSRSResult.builder()
                .stability(round(nextS))
                .difficulty(round(nextD))
                .intervalDays(intervalDays)
                .lastReviewAt(now)
                .nextReviewAt(now.plusDays(intervalDays))
                .build();
    }


    public FSRSResult calculateNextReview(Double currentStability, Double currentDifficulty, LocalDate lastReviewDate, int rating) {
        LocalDateTime lastReviewAt = lastReviewDate != null ? lastReviewDate.atStartOfDay() : null;
        return calculateNextReview(currentStability, currentDifficulty, lastReviewAt, rating);
    }


    private double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}