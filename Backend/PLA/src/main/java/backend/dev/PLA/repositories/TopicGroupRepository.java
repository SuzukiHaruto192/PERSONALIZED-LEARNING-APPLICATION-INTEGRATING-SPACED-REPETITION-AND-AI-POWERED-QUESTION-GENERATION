package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.TopicGroup;
import backend.dev.PLA.enums.TopicGroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicGroupRepository extends JpaRepository<TopicGroup, Long> {
    Optional<TopicGroup> findById(Long id);

    List<TopicGroup> findByType(TopicGroupType type);

    Optional<TopicGroup> findByNameAndType(String name, TopicGroupType type);

    boolean existsByNameAndType(String name, TopicGroupType type);

    boolean existsByNameAndTypeAndIdNot(String name, TopicGroupType type, Long id);
}
