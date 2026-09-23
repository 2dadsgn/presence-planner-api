package it.intecs.presenceplanner.repo;

import it.intecs.presenceplanner.domain.PresenceException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceExceptionRepository extends JpaRepository<PresenceException, Long> {

  List<PresenceException> findByTeamId(Long teamId);

  List<PresenceException> findByTeamIdAndUserId(Long teamId, Long userId);
}
