package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.enums.TopicGroupType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // Topic tồn tại và cặp (skillId, topicId) có trong skill_topics
    @Query("SELECT t FROM Topic t JOIN t.skills s WHERE t.id = :topicId AND s.id = :skillId")
    Optional<Topic> findByIdAndSkillId(@Param("topicId") Long topicId, @Param("skillId") Short skillId);

    // Cặp (skillId, topicId) còn trong skill_topics. FOR KEY SHARE giữ dòng đó tới hết transaction: admin gỡ cặp
    // (DELETE) phải chờ, nên cặp không bị xoá giữa lúc kiểm tra và lúc ghi user_topic_progress (FK tới skill_topics)
    @Query(
            value = "SELECT 1 FROM skill_topics WHERE skill_id = :skillId AND topic_id = :topicId FOR KEY SHARE",
            nativeQuery = true)
    Optional<Integer> lockSkillTopic(@Param("skillId") Short skillId, @Param("topicId") Long topicId);

    // Topic tồn tại và thuộc nhóm có type cho trước (vd GRAMMAR cho questions.grammar_topic_id)
    @Query("SELECT t FROM Topic t WHERE t.id = :id AND t.group.type = :type")
    Optional<Topic> findByIdAndGroupType(@Param("id") Long id, @Param("type") TopicGroupType type);

    @EntityGraph(attributePaths = {"group", "skills"})
    List<Topic> findByGroupIdOrderBySortOrderAsc(Long groupId);

    @EntityGraph(attributePaths = {"group", "skills"})
    List<Topic> findAllByOrderBySortOrderAsc();

    @Query("""
            SELECT t FROM Topic t
            JOIN FETCH t.group g
            JOIN t.skills s
            WHERE s.id = :skillId
            ORDER BY g.sortOrder, g.id, t.sortOrder, t.id
            """)
    List<Topic> findBySkillIdWithGroup(@Param("skillId") Short skillId);
}
