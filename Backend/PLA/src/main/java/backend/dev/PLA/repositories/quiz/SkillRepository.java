package backend.dev.PLA.repositories.quiz;

import backend.dev.PLA.entities.quiz.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
