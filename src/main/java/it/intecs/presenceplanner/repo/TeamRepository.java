package it.intecs.presenceplanner.repo;

import it.intecs.presenceplanner.domain.Team;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
  Optional<Team> findByManagerId(Long managerId);
}
