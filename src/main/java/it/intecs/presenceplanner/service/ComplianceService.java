package it.intecs.presenceplanner.service;

import it.intecs.presenceplanner.domain.PresenceException;
import it.intecs.presenceplanner.domain.PresenceType;
import it.intecs.presenceplanner.domain.Team;
import it.intecs.presenceplanner.domain.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Computes, per team member and per Monday-Sunday week, whether the
 * person met the team's minimum office-days policy — or the per-person
 * exception that overrides it, if one is active for that week.
 *
 * Only office days that fall within the requested month are counted, so a
 * week that spans a month boundary is judged on the days visible in that
 * month's view (matching what a manager reviewing "September" would see).
 */
@Service
public class ComplianceService {

  public record WeekCompliance(
      LocalDate weekStart,
      LocalDate weekEnd,
      int officeDays,
      int requiredMinimum,
      boolean exempt,
      boolean compliant) {}

  public record MemberCompliance(
      Long userId, String email, String displayName, List<WeekCompliance> weeks) {}

  public List<MemberCompliance> compute(
      Team team,
      List<User> members,
      YearMonth month,
      Map<Long, Map<LocalDate, PresenceType>> entriesByUserId,
      Map<Long, List<PresenceException>> exceptionsByUserId) {

    List<LocalDate[]> weeks = weeksOverlapping(month);

    List<MemberCompliance> result = new ArrayList<>();
    for (User member : members) {
      Map<LocalDate, PresenceType> entries =
          entriesByUserId.getOrDefault(member.getId(), Map.of());
      List<PresenceException> exceptions =
          exceptionsByUserId.getOrDefault(member.getId(), List.of());

      List<WeekCompliance> weekResults = new ArrayList<>();
      for (LocalDate[] range : weeks) {
        LocalDate weekStart = range[0];
        LocalDate weekEnd = range[1];

        long officeDays =
            entries.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(weekStart) && !e.getKey().isAfter(weekEnd))
                .filter(e -> e.getValue() == PresenceType.OFFICE)
                .count();

        PresenceException active =
            exceptions.stream()
                .filter(ex -> ex.coversWeek(weekStart, weekEnd))
                .max(Comparator.comparing(PresenceException::getCreatedAt))
                .orElse(null);

        boolean exempt = active != null && active.getMinOfficeDaysPerWeek() == null;
        int requiredMinimum =
            exempt
                ? 0
                : active != null
                    ? active.getMinOfficeDaysPerWeek()
                    : team.getMinOfficeDaysPerWeek();
        boolean compliant = exempt || officeDays >= requiredMinimum;

        weekResults.add(
            new WeekCompliance(
                weekStart, weekEnd, (int) officeDays, requiredMinimum, exempt, compliant));
      }

      result.add(
          new MemberCompliance(
              member.getId(), member.getEmail(), member.getDisplayName(), weekResults));
    }
    return result;
  }

  /** Monday-Sunday [start, end] ranges for every week that touches the given month, in order. */
  private List<LocalDate[]> weeksOverlapping(YearMonth month) {
    Set<LocalDate> weekStarts = new LinkedHashSet<>();
    LocalDate date = month.atDay(1);
    LocalDate end = month.atEndOfMonth();
    while (!date.isAfter(end)) {
      weekStarts.add(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
      date = date.plusDays(1);
    }
    List<LocalDate[]> weeks = new ArrayList<>();
    for (LocalDate start : weekStarts) {
      weeks.add(new LocalDate[] {start, start.plusDays(6)});
    }
    return weeks;
  }
}
