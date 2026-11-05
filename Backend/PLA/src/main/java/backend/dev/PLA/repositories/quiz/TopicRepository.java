package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Topic;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

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
    List<Topic> findBySkillIdWithGroup(@Param("skillId") Long skillId);
}
