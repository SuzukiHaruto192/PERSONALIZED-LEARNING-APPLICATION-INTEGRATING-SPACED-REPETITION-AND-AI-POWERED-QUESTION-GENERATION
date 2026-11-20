package backend.dev.PLA.repositories.flashcard;

import backend.dev.PLA.entities.flashcard.FlashCard;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashCardRepository extends JpaRepository<FlashCard, Long> {
    @Query(value = """
               SELECT DISTINCT fc.*
               FROM flashcard fc
               INNER JOIN flashcard_detail fcd ON fc.id = fcd.flashcard_id
               INNER JOIN flashcard_decks d ON fcd.deck_id = d.id
               INNER JOIN users u ON d.user_id = u.id
               WHERE u.username = :username
          """, nativeQuery = true)
    List<FlashCard> findAllByUsername(@Param("username") String username);

    @Query(value = """
               SELECT fc.*
               FROM flashcard fc
               INNER JOIN flashcard_detail fcd ON fc.id = fcd.flashcard_id
               WHERE fcd.deck_id = :deckId
          """, nativeQuery = true)
    List<FlashCard> findAllByDeckId(@Param("deckId") Long deckId);

    /** Thẻ do chính user tạo, trùng nội dung (không phân biệt hoa thường). Không tái sử dụng thẻ của người khác. */
    @Query(value = """
            SELECT f.*
            FROM flashcard f
            WHERE f.created_by = :userId
              AND LOWER(f.front_content) = LOWER(:frontContent)
              AND LOWER(f.back_content) = LOWER(:backContent)
            ORDER BY f.id
            LIMIT 1
            """, nativeQuery = true)
    Optional<FlashCard> findByOwnerAndContent(
            @Param("userId") Long userId,
            @Param("frontContent") String frontContent,
            @Param("backContent") String backContent);

    /** Có thẻ khác (id khác {@code excludeId}) trùng nội dung đang nằm trong deck của user không. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM flashcard f
                JOIN flashcard_detail fcd ON fcd.flashcard_id = f.id
                JOIN flashcard_decks d ON d.id = fcd.deck_id
                WHERE d.user_id = :userId
                  AND f.id <> :excludeId
                  AND LOWER(f.front_content) = LOWER(:frontContent)
                  AND LOWER(f.back_content) = LOWER(:backContent)
            )
            """, nativeQuery = true)
    boolean existsOtherInUserDecksByContent(
            @Param("userId") Long userId,
            @Param("frontContent") String frontContent,
            @Param("backContent") String backContent,
            @Param("excludeId") Long excludeId);

    /** Xoá các thẻ (trong {@code ids}) không còn nằm trong deck nào của bất kỳ ai (thẻ mồ côi). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM flashcard f
            WHERE f.id IN (:ids)
              AND NOT EXISTS (SELECT 1 FROM flashcard_detail d WHERE d.flashcard_id = f.id)
            """, nativeQuery = true)
    int deleteOrphansIn(@Param("ids") Collection<Long> ids);
}
