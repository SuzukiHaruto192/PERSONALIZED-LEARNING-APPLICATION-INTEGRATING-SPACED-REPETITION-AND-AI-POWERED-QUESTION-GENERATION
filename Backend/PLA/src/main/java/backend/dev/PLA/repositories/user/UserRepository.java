package backend.dev.PLA.repositories.user;

import backend.dev.PLA.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    Long findIdByUsername(String username);
}
