package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Question;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    long countByQuizId(Long quizId);

    // Mỗi phần tử: [quizId (Long), số câu hỏi (Long)]
    @Query("SELECT q.quiz.id, COUNT(q) FROM Question q WHERE q.quiz.id IN :quizIds GROUP BY q.quiz.id")
    List<Object[]> countByQuizIds(@Param("quizIds") Collection<Long> quizIds);

    long countByPassageId(Long passageId);

    boolean existsByPassageId(Long passageId);

    // Mỗi phần tử: [passageId (Long), số câu hỏi (Long)]
    @Query("SELECT q.passage.id, COUNT(q) FROM Question q WHERE q.passage.id IN :passageIds GROUP BY q.passage.id")
    List<Object[]> countByPassageIds(@Param("passageIds") Collection<Long> passageIds);

    // 0 khi quiz chưa có câu hỏi nào
    @Query("SELECT COALESCE(MAX(q.position), 0) FROM Question q WHERE q.quiz.id = :quizId")
    int findMaxPositionByQuizId(@Param("quizId") Long quizId);

    boolean existsByQuizIdAndPosition(Long quizId, Short position);

    boolean existsByQuizIdAndPositionAndIdNot(Long quizId, Short position, Long id);

    @Query("SELECT q.id FROM Question q WHERE q.quiz.id = :quizId")
    List<Long> findIdsByQuizId(@Param("quizId") Long quizId);

    // Câu hỏi kèm options và grammarTopic trong một query (tránh N+1), sắp theo position
    @Query("""
            SELECT q FROM Question q
            LEFT JOIN FETCH q.options o
            LEFT JOIN FETCH q.grammarTopic
            WHERE q.quiz.id = :quizId
            ORDER BY q.position, o.position
            """)
    List<Question> findDetailsByQuizId(@Param("quizId") Long quizId);

    @Query("""
            SELECT q FROM Question q
            LEFT JOIN FETCH q.options o
            LEFT JOIN FETCH q.grammarTopic
            WHERE q.quiz.id = :quizId AND q.passage.id = :passageId
            ORDER BY q.position, o.position
            """)
    List<Question> findDetailsByQuizIdAndPassageId(@Param("quizId") Long quizId, @Param("passageId") Long passageId);

    @Query("""
            SELECT q FROM Question q
            JOIN FETCH q.quiz
            LEFT JOIN FETCH q.options o
            LEFT JOIN FETCH q.grammarTopic
            WHERE q.id = :id
            ORDER BY o.position
            """)
    Optional<Question> findDetailsById(@Param("id") Long id);

    // Bước 1 khi sắp xếp: đổi mọi position sang số âm để bước 2 không đụng UNIQUE (quiz_id, position)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Question q SET q.position = -q.position WHERE q.quiz.id = :quizId")
    int negatePositionsByQuizId(@Param("quizId") Long quizId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Question q SET q.position = :position WHERE q.id = :id")
    int updatePosition(@Param("id") Long id, @Param("position") Short position);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Question q WHERE q.id = :id")
    int deleteByIdDirectly(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Question q WHERE q.quiz.id = :quizId")
    int deleteAllByQuizId(@Param("quizId") Long quizId);
}
