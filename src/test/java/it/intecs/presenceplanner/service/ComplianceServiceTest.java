package it.intecs.presenceplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import it.intecs.presenceplanner.domain.PresenceException;
import it.intecs.presenceplanner.domain.PresenceType;
import it.intecs.presenceplanner.domain.Team;
import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.service.ComplianceService.MemberCompliance;
import it.intecs.presenceplanner.service.ComplianceService.WeekCompliance;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComplianceServiceTest {

  private final ComplianceService compliance = new ComplianceService();

  private User user(long id) {
    User u = new User();
    u.setId(id);
    u.setEmail("user" + id + "@example.com");
    u.setDisplayName("User " + id);
    return u;
  }

  private Team team(int minOfficeDaysPerWeek) {
    Team t = new Team();
    t.setId(1L);
    t.setName("Test team");
    t.setMinOfficeDaysPerWeek(minOfficeDaysPerWeek);
    return t;
  }

  @Test
  void meetsTeamMinimumWhenEnoughOfficeDaysInWeek() {
    // September 2026: Mon 7 - Sun 13 is one full week inside the month.
    Team team = team(2);
    User u = user(1);
    Map<LocalDate, PresenceType> entries =
        Map.of(
            LocalDate.of(2026, 9, 7), PresenceType.OFFICE,
            LocalDate.of(2026, 9, 8), PresenceType.OFFICE,
            LocalDate.of(2026, 9, 9), PresenceType.REMOTE);

    List<MemberCompliance> result =
        compliance.compute(
            team,
            List.of(u),
            YearMonth.of(2026, 9),
            Map.of(1L, entries),
            Map.of());

    WeekCompliance week = weekStarting(result.get(0).weeks(), LocalDate.of(2026, 9, 7));
    assertThat(week.officeDays()).isEqualTo(2);
    assertThat(week.requiredMinimum()).isEqualTo(2);
    assertThat(week.compliant()).isTrue();
    assertThat(week.exempt()).isFalse();
  }

  @Test
  void failsTeamMinimumWhenNotEnoughOfficeDays() {
    Team team = team(3);
    User u = user(1);
    Map<LocalDate, PresenceType> entries =
        Map.of(LocalDate.of(2026, 9, 7), PresenceType.OFFICE);

    List<MemberCompliance> result =
        compliance.compute(
            team, List.of(u), YearMonth.of(2026, 9), Map.of(1L, entries), Map.of());

    WeekCompliance week = weekStarting(result.get(0).weeks(), LocalDate.of(2026, 9, 7));
    assertThat(week.officeDays()).isEqualTo(1);
    assertThat(week.requiredMinimum()).isEqualTo(3);
    assertThat(week.compliant()).isFalse();
  }

  @Test
  void fullExemptionOverridesTeamMinimumRegardlessOfOfficeDays() {
    Team team = team(5);
    User u = user(1);
    User manager = user(99);

    PresenceException exemption = new PresenceException();
    exemption.setTeam(team);
    exemption.setUser(u);
    exemption.setMinOfficeDaysPerWeek(null); // fully exempt
    exemption.setStartDate(LocalDate.of(2026, 9, 1));
    exemption.setEndDate(LocalDate.of(2026, 9, 30));
    exemption.setReason("Parental leave");
    exemption.setCreatedBy(manager);
    exemption.setCreatedAt(Instant.now());

    List<MemberCompliance> result =
        compliance.compute(
            team,
            List.of(u),
            YearMonth.of(2026, 9),
            Map.of(1L, Map.of()), // zero office days
            Map.of(1L, List.of(exemption)));

    WeekCompliance week = weekStarting(result.get(0).weeks(), LocalDate.of(2026, 9, 7));
    assertThat(week.exempt()).isTrue();
    assertThat(week.compliant()).isTrue();
    assertThat(week.requiredMinimum()).isZero();
  }

  @Test
  void personalExceptionOverridesTeamMinimumWithALowerValue() {
    Team team = team(4);
    User u = user(1);
    User manager = user(99);

    PresenceException reduced = new PresenceException();
    reduced.setTeam(team);
    reduced.setUser(u);
    reduced.setMinOfficeDaysPerWeek(1);
    reduced.setStartDate(LocalDate.of(2026, 9, 1));
    reduced.setEndDate(null); // open-ended
    reduced.setReason("Reduced schedule, agreed with manager");
    reduced.setCreatedBy(manager);
    reduced.setCreatedAt(Instant.now());

    Map<LocalDate, PresenceType> entries = Map.of(LocalDate.of(2026, 9, 7), PresenceType.OFFICE);

    List<MemberCompliance> result =
        compliance.compute(
            team,
            List.of(u),
            YearMonth.of(2026, 9),
            Map.of(1L, entries),
            Map.of(1L, List.of(reduced)));

    WeekCompliance week = weekStarting(result.get(0).weeks(), LocalDate.of(2026, 9, 7));
    assertThat(week.requiredMinimum()).isEqualTo(1);
    assertThat(week.compliant()).isTrue();
    assertThat(week.exempt()).isFalse();
  }

  @Test
  void exceptionOutsideItsDateRangeDoesNotApply() {
    Team team = team(3);
    User u = user(1);
    User manager = user(99);

    PresenceException octoberOnly = new PresenceException();
    octoberOnly.setTeam(team);
    octoberOnly.setUser(u);
    octoberOnly.setMinOfficeDaysPerWeek(0);
    octoberOnly.setStartDate(LocalDate.of(2026, 10, 1));
    octoberOnly.setEndDate(LocalDate.of(2026, 10, 31));
    octoberOnly.setReason("Different month entirely");
    octoberOnly.setCreatedBy(manager);
    octoberOnly.setCreatedAt(Instant.now());

    List<MemberCompliance> result =
        compliance.compute(
            team,
            List.of(u),
            YearMonth.of(2026, 9),
            Map.of(1L, Map.of()),
            Map.of(1L, List.of(octoberOnly)));

    WeekCompliance week = weekStarting(result.get(0).weeks(), LocalDate.of(2026, 9, 7));
    assertThat(week.exempt()).isFalse();
    assertThat(week.requiredMinimum()).isEqualTo(3); // falls back to the team policy
  }

  @Test
  void coversAllWeeksTouchingTheMonthIncludingLeadAndTrailDays() {
    // September 2026 starts on a Tuesday, so the grid's first week starts Monday 31 August.
    Team team = team(0);
    User u = user(1);

    List<MemberCompliance> result =
        compliance.compute(
            team, List.of(u), YearMonth.of(2026, 9), Map.of(1L, Map.of()), Map.of());

    List<WeekCompliance> weeks = result.get(0).weeks();
    assertThat(weeks.getFirst().weekStart()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(weeks.getLast().weekEnd()).isAfterOrEqualTo(LocalDate.of(2026, 9, 30));
  }

  private WeekCompliance weekStarting(List<WeekCompliance> weeks, LocalDate start) {
    return weeks.stream()
        .filter(w -> w.weekStart().equals(start))
        .findFirst()
        .orElseThrow();
  }
}
