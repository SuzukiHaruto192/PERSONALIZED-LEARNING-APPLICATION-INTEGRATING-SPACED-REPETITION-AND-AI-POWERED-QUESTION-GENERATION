package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.TopicGroup;
import backend.dev.PLA.enums.TopicGroupType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicGroupRepository extends JpaRepository<TopicGroup, Long> {
    Optional<TopicGroup> findById(Long id);

    List<TopicGroup> findByType(TopicGroupType type);

    Optional<TopicGroup> findByNameAndType(String name, TopicGroupType type);

    boolean existsByNameAndType(String name, TopicGroupType type);

    boolean existsByNameAndTypeAndIdNot(String name, TopicGroupType type, Long id);

    //    @Query("""
    //            SELECT DISTINCT tg FROM TopicGroup tg
    //            JOIN tg.topics t
    //            JOIN t.skills s
    //            WHERE s.id = :skillId
    //            """)
    //    List<TopicGroup> selectTopicGroupBySkillId(@Param("skillId") Long skillId);
}
