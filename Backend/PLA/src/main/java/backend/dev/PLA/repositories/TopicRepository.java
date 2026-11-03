package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.Topic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic,Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = "group")
    List<Topic> findByGroupIdOrderBySortOrderAsc(Long groupId);

    @EntityGraph(attributePaths = "group")
    List<Topic> findAllByOrderBySortOrderAsc();
}
