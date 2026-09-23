package it.intecs.presenceplanner.repo;

import it.intecs.presenceplanner.domain.PresenceEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceEntryRepository extends JpaRepository<PresenceEntry, Long> {

  List<PresenceEntry> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

  List<PresenceEntry> findByUserIdInAndDateBetween(
      List<Long> userIds, LocalDate start, LocalDate end);

  Optional<PresenceEntry> findByUserIdAndDate(Long userId, LocalDate date);

  void deleteByUserIdAndDateIn(Long userId, List<LocalDate> dates);
}
