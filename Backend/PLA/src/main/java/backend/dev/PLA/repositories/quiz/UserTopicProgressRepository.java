package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.UserTopicProgress;
import backend.dev.PLA.entities.quiz.UserTopicProgressId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTopicProgressRepository extends JpaRepository<UserTopicProgress, UserTopicProgressId> {

    // Tạo dòng mặc định (NEW, chưa có lịch) nếu chưa có. Dùng ON CONFLICT DO NOTHING thay cho bắt
    // DataIntegrityViolationException: trên Postgres lỗi unique làm hỏng cả transaction nên không đọc lại được.
    // Hai lượt nộp cùng lúc: lượt sau chờ lượt trước commit rồi bỏ qua insert.
    @Modifying
    @Query(
            value = "INSERT INTO user_topic_progress (user_id, skill_id, topic_id) "
                    + "VALUES (:userId, :skillId, :topicId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("skillId") Short skillId, @Param("topicId") Long topicId);

    // SELECT ... FOR UPDATE: các lượt nộp cùng cặp (user, skill, topic) chạy FSRS lần lượt, không ghi đè nhau
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UserTopicProgress p WHERE p.id = :id")
    Optional<UserTopicProgress> findByIdForUpdate(@Param("id") UserTopicProgressId id);

    /**
     * Cặp (skill, topic) đến hạn ôn (next_review <= today), quá hạn lâu nhất trước, cùng ngày thì mastery thấp trước.
     * Dùng index idx_progress_due. Không JOIN FETCH nên câu đếm cho Page do Spring tự sinh, không cần countQuery.
     * skill / topic là LAZY: được nạp khi mapper truy cập (trong transaction của service).
     */
    @Query("""
            SELECT p FROM UserTopicProgress p
            WHERE p.id.userId = :userId
              AND p.nextReviewDate <= :today
            ORDER BY p.nextReviewDate ASC, p.mastery ASC, p.id.skillId ASC, p.id.topicId ASC
            """)
    Page<UserTopicProgress> findDue(@Param("userId") Long userId, @Param("today") LocalDate today, Pageable pageable);
}
