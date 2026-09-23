package it.intecs.presenceplanner.repo;

import it.intecs.presenceplanner.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmailIgnoreCase(String email);

  Optional<User> findByExternalId(String externalId);

  List<User> findByTeamId(Long teamId);
}
