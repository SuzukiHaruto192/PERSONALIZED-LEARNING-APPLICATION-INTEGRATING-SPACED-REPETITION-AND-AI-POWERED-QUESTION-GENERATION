package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Quiz;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {
    Quiz findByTitle(String title);

    // Nạp sẵn skill/topic/format để build response không bị N+1
    @EntityGraph(attributePaths = {"createdBy", "skill", "topic", "format"})
    Optional<Quiz> findWithRefsById(Long id);

    // FOR UPDATE (không phải FOR NO KEY UPDATE) xung đột với khoá KEY SHARE mà FK của quiz_attempts lấy khi insert,
    // nên không có attempt mới chen vào giữa lúc kiểm tra và lúc xoá
    @Query(value = "SELECT * FROM quizzes WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Quiz> findByIdForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "skill", "topic", "format"})
    Page<Quiz> findAll(Specification<Quiz> spec, Pageable pageable);
}
