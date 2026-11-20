package backend.dev.PLA.repositories.flashcard;

import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.flashcard.FlashCardDetailId;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashCardDetailRepository extends JpaRepository<FlashCardDetail, FlashCardDetailId> {
    boolean existsById(FlashCardDetailId flashCardDetailId);

    Optional<FlashCardDetail> findById(FlashCardDetailId flashCardDetailId);

    // ===== Phiên ôn (deckId = null: mọi deck của user) =====

    /**
     * Thẻ đến hạn (next_review <= today), quá hạn lâu nhất đứng trước. Thẻ mới (next_review NULL) không nằm trong
     * kết quả. Có JOIN FETCH nên phải khai báo countQuery riêng.
     */
    @Query(value = """
                    SELECT d FROM FlashCardDetail d
                    JOIN FETCH d.flashCard
                    JOIN FETCH d.deck dk
                    WHERE dk.user.id = :userId
                      AND (:deckId IS NULL OR dk.id = :deckId)
                      AND d.nextReviewDate <= :today
                    ORDER BY d.nextReviewDate ASC, d.addedAt ASC, d.id.deckId ASC, d.id.flashcardId ASC
                    """, countQuery = """
                    SELECT COUNT(d) FROM FlashCardDetail d
                    WHERE d.deck.user.id = :userId
                      AND (:deckId IS NULL OR d.deck.id = :deckId)
                      AND d.nextReviewDate <= :today
                    """)
    Page<FlashCardDetail> findDue(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("today") LocalDate today,
            Pageable pageable);

    // ===== Deck =====

    @Query(value = """
                    SELECT d FROM FlashCardDetail d
                    JOIN FETCH d.flashCard
                    JOIN FETCH d.deck
                    WHERE d.deck.id = :deckId
                    ORDER BY d.addedAt ASC, d.id.flashcardId ASC
                    """, countQuery = "SELECT COUNT(d) FROM FlashCardDetail d WHERE d.deck.id = :deckId")
    Page<FlashCardDetail> findPageByDeckId(@Param("deckId") Long deckId, Pageable pageable);

    @Query(value = """
                    SELECT COUNT(*)                                          AS "totalCards",
                           COUNT(*) FILTER (WHERE next_review <= :today)     AS "dueToday",
                           COUNT(*) FILTER (WHERE fsrs_state = 'NEW')        AS "newCards",
                           COUNT(*) FILTER (WHERE fsrs_state = 'LEARNING')   AS "learning",
                           COUNT(*) FILTER (WHERE fsrs_state = 'REVIEW')     AS "review",
                           COUNT(*) FILTER (WHERE fsrs_state = 'RELEARNING') AS "relearning"
                    FROM flashcard_detail
                    WHERE deck_id = :deckId
                    """, nativeQuery = true)
    DeckStatsView countStatsByDeck(@Param("deckId") Long deckId, @Param("today") LocalDate today);

    /** Id các thẻ (trong {@code flashCardIds}) đã có trong deck. */
    @Query("SELECT d.id.flashcardId FROM FlashCardDetail d "
            + "WHERE d.id.deckId = :deckId AND d.id.flashcardId IN :flashCardIds")
    List<Long> findFlashCardIdsInDeck(
            @Param("deckId") Long deckId, @Param("flashCardIds") Collection<Long> flashCardIds);

    /** Id các thẻ đang nằm trong deck (lấy trước khi xoá deck để dọn thẻ mồ côi). */
    @Query("SELECT d.id.flashcardId FROM FlashCardDetail d WHERE d.id.deckId = :deckId")
    List<Long> findFlashCardIdsByDeckId(@Param("deckId") Long deckId);

    // ===== Thẻ của user =====

    /** Các dòng (deck, thẻ) của thẻ này nằm trong deck của user. */
    @Query("SELECT d FROM FlashCardDetail d JOIN FETCH d.flashCard "
            + "WHERE d.flashCard.id = :flashCardId AND d.deck.user.id = :userId")
    List<FlashCardDetail> findAllByFlashCardIdAndUserId(
            @Param("flashCardId") Long flashCardId, @Param("userId") Long userId);

    /** Thẻ còn nằm trong deck của user khác (thẻ dùng chung từ dữ liệu cũ). */
    @Query("SELECT COUNT(d) > 0 FROM FlashCardDetail d "
            + "WHERE d.flashCard.id = :flashCardId AND d.deck.user.id <> :userId")
    boolean existsByFlashCardIdAndDeckUserIdNot(@Param("flashCardId") Long flashCardId, @Param("userId") Long userId);

    /** Id các thẻ (trong {@code flashCardIds}) đang nằm trong ít nhất một deck của user. */
    @Query("SELECT DISTINCT d.id.flashcardId FROM FlashCardDetail d "
            + "WHERE d.deck.user.id = :userId AND d.id.flashcardId IN :flashCardIds")
    List<Long> findOwnedFlashCardIds(
            @Param("userId") Long userId, @Param("flashCardIds") Collection<Long> flashCardIds);

    /** Copy-on-write: chuyển các dòng của user sang thẻ mới, giữ nguyên lịch ôn. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    UPDATE flashcard_detail SET flashcard_id = :newFlashCardId
                    WHERE flashcard_id = :oldFlashCardId
                      AND deck_id IN (SELECT id FROM flashcard_decks WHERE user_id = :userId)
                    """, nativeQuery = true)
    int moveToFlashCard(
            @Param("oldFlashCardId") Long oldFlashCardId,
            @Param("newFlashCardId") Long newFlashCardId,
            @Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    DELETE FROM flashcard_detail
                    WHERE flashcard_id = :flashCardId
                      AND deck_id IN (SELECT id FROM flashcard_decks WHERE user_id = :userId)
                    """, nativeQuery = true)
    int deleteByFlashCardIdAndUserId(@Param("flashCardId") Long flashCardId, @Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM FlashCardDetail d WHERE d.id.deckId = :deckId AND d.id.flashcardId = :flashCardId")
    int deleteByDeckIdAndFlashCardId(@Param("deckId") Long deckId, @Param("flashCardId") Long flashCardId);
}
