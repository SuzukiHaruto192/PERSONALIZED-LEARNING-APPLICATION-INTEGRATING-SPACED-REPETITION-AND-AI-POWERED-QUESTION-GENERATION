package backend.dev.PLA.repositories;

import backend.dev.PLA.entities.TokenExpired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenExpiredRepository  extends JpaRepository<TokenExpired,Long> {
    TokenExpired findById(String id);
    Boolean existsById(String id);
}
