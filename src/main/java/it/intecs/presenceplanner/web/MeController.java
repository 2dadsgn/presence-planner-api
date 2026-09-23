package it.intecs.presenceplanner.web;

import it.intecs.presenceplanner.domain.Team;
import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.repo.TeamRepository;
import it.intecs.presenceplanner.service.CurrentUserService;
import it.intecs.presenceplanner.web.dto.MeResponse;
import it.intecs.presenceplanner.web.dto.MeResponse.TeamSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Who am I, and do I manage a team? The Angular app uses this right after login. */
@RestController
@RequestMapping("/api/me")
public class MeController {

  private final CurrentUserService currentUserService;
  private final TeamRepository teamRepository;

  public MeController(CurrentUserService currentUserService, TeamRepository teamRepository) {
    this.currentUserService = currentUserService;
    this.teamRepository = teamRepository;
  }

  @GetMapping
  public MeResponse me() {
    User user = currentUserService.currentUser();

    TeamSummary team =
        user.getTeam() == null ? null : toSummary(user.getTeam());

    TeamSummary managedTeam =
        teamRepository.findByManagerId(user.getId()).map(this::toSummary).orElse(null);

    return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), team, managedTeam);
  }

  private TeamSummary toSummary(Team team) {
    return new TeamSummary(team.getId(), team.getName(), team.getMinOfficeDaysPerWeek());
  }
}
