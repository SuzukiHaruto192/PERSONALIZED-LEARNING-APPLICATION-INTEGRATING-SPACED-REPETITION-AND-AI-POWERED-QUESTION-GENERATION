package backend.dev.PLA.repositories.flashcard;

/** Kết quả thống kê một deck (projection cho native query {@code countStatsByDeck}). */
public interface DeckStatsView {
    Long getTotalCards();

    Long getDueToday();

    Long getNewCards();

    Long getLearning();

    Long getReview();

    Long getRelearning();
}
