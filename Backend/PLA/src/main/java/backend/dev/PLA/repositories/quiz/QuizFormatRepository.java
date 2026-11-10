package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.QuizFormat;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizFormatRepository extends JpaRepository<QuizFormat, Short> {
    // Tương ứng FK kép (format_id, skill_id) -> quiz_formats(id, skill_id)
    Optional<QuizFormat> findByIdAndSkillId(Short id, Long skillId);
}
