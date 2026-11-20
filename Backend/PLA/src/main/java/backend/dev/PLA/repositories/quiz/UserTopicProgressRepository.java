package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.UserTopicProgress;
import backend.dev.PLA.entities.quiz.UserTopicProgressId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
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
}
