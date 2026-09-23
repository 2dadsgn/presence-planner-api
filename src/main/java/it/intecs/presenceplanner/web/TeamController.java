package it.intecs.presenceplanner.web;

import it.intecs.presenceplanner.domain.PresenceException;
import it.intecs.presenceplanner.domain.PresenceType;
import it.intecs.presenceplanner.domain.Team;
import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.service.ComplianceService;
import it.intecs.presenceplanner.service.CurrentUserService;
import it.intecs.presenceplanner.service.PresenceService;
import it.intecs.presenceplanner.service.TeamService;
import it.intecs.presenceplanner.web.dto.TeamDtos.CreateExceptionRequest;
import it.intecs.presenceplanner.web.dto.TeamDtos.ExceptionDto;
import it.intecs.presenceplanner.web.dto.TeamDtos.MemberDto;
import it.intecs.presenceplanner.web.dto.TeamDtos.MemberMonthDto;
import it.intecs.presenceplanner.web.dto.TeamDtos.PolicyDto;
import it.intecs.presenceplanner.web.dto.TeamDtos.TeamMonthResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints only a team's manager can use: see members' calendars, judge
 * them against the minimum-office-days policy, and configure that policy
 * (including per-person exceptions). Every method re-checks that the
 * caller actually manages the requested team — see TeamService.assertManagesTeam.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

  private final TeamService teamService;
  private final PresenceService presenceService;
  private final ComplianceService complianceService;
  private final CurrentUserService currentUserService;

  public TeamController(
      TeamService teamService,
      PresenceService presenceService,
      ComplianceService complianceService,
      CurrentUserService currentUserService) {
    this.teamService = teamService;
    this.presenceService = presenceService;
    this.complianceService = complianceService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/mine")
  public Map<String, Object> mine() {
    Team team = teamService.getManagedTeam(currentUserService.currentUser());
    return Map.of(
        "id", team.getId(),
        "name", team.getName(),
        "minOfficeDaysPerWeek", team.getMinOfficeDaysPerWeek());
  }

  @GetMapping("/{teamId}/members")
  public List<MemberDto> members(@PathVariable Long teamId) {
    Team team = teamService.assertManagesTeam(currentUserService.currentUser(), teamId);
    return teamService.listMembers(team).stream()
        .map(u -> new MemberDto(u.getId(), u.getEmail(), u.getDisplayName()))
        .toList();
  }

  @PutMapping("/{teamId}/policy")
  public PolicyDto updatePolicy(@PathVariable Long teamId, @Valid @RequestBody PolicyDto request) {
    Team team = teamService.assertManagesTeam(currentUserService.currentUser(), teamId);
    Team updated = teamService.updatePolicy(team, request.minOfficeDaysPerWeek());
    return new PolicyDto(updated.getMinOfficeDaysPerWeek());
  }

  @GetMapping("/{teamId}/presence")
  public TeamMonthResponse teamPresence(
      @PathVariable Long teamId, @RequestParam int year, @RequestParam int month) {
    Team team = teamService.assertManagesTeam(currentUserService.currentUser(), teamId);
    List<User> members = teamService.listMembers(team);
    YearMonth ym = YearMonth.of(year, month);

    List<Long> memberIds = members.stream().map(User::getId).toList();
    Map<Long, Map<LocalDate, PresenceType>> entriesByUser =
        presenceService.getMonthForUsers(memberIds, ym);

    List<PresenceException> allExceptions = teamService.listExceptions(team);
    Map<Long, List<PresenceException>> exceptionsByUser =
        allExceptions.stream()
            .collect(java.util.stream.Collectors.groupingBy(e -> e.getUser().getId()));

    List<ComplianceService.MemberCompliance> compliance =
        complianceService.compute(team, members, ym, entriesByUser, exceptionsByUser);

    List<MemberMonthDto> memberDtos =
        compliance.stream()
            .map(
                c ->
                    new MemberMonthDto(
                        c.userId(),
                        c.email(),
                        c.displayName(),
                        entriesByUser.getOrDefault(c.userId(), Map.of()),
                        c.weeks()))
            .toList();

    return new TeamMonthResponse(
        team.getId(), team.getName(), team.getMinOfficeDaysPerWeek(), ym.toString(), memberDtos);
  }

  @GetMapping("/{teamId}/exceptions")
  public List<ExceptionDto> listExceptions(@PathVariable Long teamId) {
    Team team = teamService.assertManagesTeam(currentUserService.currentUser(), teamId);
    return teamService.listExceptions(team).stream().map(this::toDto).toList();
  }

  @PostMapping("/{teamId}/exceptions")
  public ExceptionDto createException(
      @PathVariable Long teamId, @Valid @RequestBody CreateExceptionRequest request) {
    User manager = currentUserService.currentUser();
    Team team = teamService.assertManagesTeam(manager, teamId);
    PresenceException exception =
        teamService.createException(
            team,
            manager,
            request.userId(),
            request.minOfficeDaysPerWeek(),
            request.startDate(),
            request.endDate(),
            request.reason());
    return toDto(exception);
  }

  @DeleteMapping("/{teamId}/exceptions/{exceptionId}")
  public void deleteException(@PathVariable Long teamId, @PathVariable Long exceptionId) {
    Team team = teamService.assertManagesTeam(currentUserService.currentUser(), teamId);
    teamService.deleteException(team, exceptionId);
  }

  private ExceptionDto toDto(PresenceException e) {
    return new ExceptionDto(
        e.getId(),
        e.getUser().getId(),
        e.getUser().getDisplayName(),
        e.getMinOfficeDaysPerWeek(),
        e.getStartDate(),
        e.getEndDate(),
        e.getReason(),
        e.getCreatedBy().getDisplayName());
  }
}
