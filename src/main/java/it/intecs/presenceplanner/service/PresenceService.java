package it.intecs.presenceplanner.service;

import it.intecs.presenceplanner.domain.PresenceEntry;
import it.intecs.presenceplanner.domain.PresenceType;
import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.repo.PresenceEntryRepository;
import it.intecs.presenceplanner.web.dto.PresenceDtos;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.intecs.presenceplanner.web.dto.PresenceDtos.AssignRequest;

@Service
public class PresenceService {

  private final PresenceEntryRepository presenceEntryRepository;

  public PresenceService(PresenceEntryRepository presenceEntryRepository) {
    this.presenceEntryRepository = presenceEntryRepository;
  }

  /** date -> type, for one user's given month. */
  public List<PresenceDtos.DateEntry> getMonth( User user, YearMonth month) {
    List<PresenceEntry> entries =
        presenceEntryRepository.findByUserIdAndDateBetween(
            user.getId(), month.atDay(1), month.atEndOfMonth());

    List<PresenceDtos.DateEntry> presences = entries.stream().map(e -> new PresenceDtos.DateEntry(e.getDate(), e.getType())).toList();

    return presences;
  }

  /** date -> type for several users at once (team view), keyed by user id. */
  public Map<Long, Map<LocalDate, PresenceType>> getMonthForUsers(
      List<Long> userIds, YearMonth month) {
    List<PresenceEntry> entries =
        presenceEntryRepository.findByUserIdInAndDateBetween(
            userIds, month.atDay(1), month.atEndOfMonth());
    return entries.stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getUser().getId(),
                Collectors.toMap(PresenceEntry::getDate, PresenceEntry::getType)));
  }

  //todo aggiungere logica di eccezioni e regole
  @Transactional
  public void assign(User user, AssignRequest presences) {
    for ( PresenceDtos.DateEntry date : presences.presences()) {
      PresenceEntry entry =
          presenceEntryRepository
              .findByUserIdAndDate(user.getId(), date.date())
              .orElseGet(() -> new PresenceEntry(user, date.date(), date.type()));
      entry.setType(date.type());
      entry.setUpdatedAt(Instant.now());
      presenceEntryRepository.save(entry);
    }
  }

  @Transactional
  public void clear(User user, List<LocalDate> dates) {
    presenceEntryRepository.deleteByUserIdAndDateIn(user.getId(), dates);
  }
}
