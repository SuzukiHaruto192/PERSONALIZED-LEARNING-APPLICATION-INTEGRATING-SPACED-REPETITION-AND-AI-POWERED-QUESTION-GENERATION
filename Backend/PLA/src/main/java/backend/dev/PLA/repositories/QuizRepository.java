package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz,Long> {
    Quiz findByTitle(String title);
}
