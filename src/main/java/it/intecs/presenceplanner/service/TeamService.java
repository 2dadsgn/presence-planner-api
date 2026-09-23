package it.intecs.presenceplanner.service;

import it.intecs.presenceplanner.domain.PresenceException;
import it.intecs.presenceplanner.domain.Team;
import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.repo.PresenceExceptionRepository;
import it.intecs.presenceplanner.repo.TeamRepository;
import it.intecs.presenceplanner.repo.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final PresenceExceptionRepository exceptionRepository;

  public TeamService(
      TeamRepository teamRepository,
      UserRepository userRepository,
      PresenceExceptionRepository exceptionRepository) {
    this.teamRepository = teamRepository;
    this.userRepository = userRepository;
    this.exceptionRepository = exceptionRepository;
  }

  /** The team the given user manages, or throws if they don't manage one. */
  public Team getManagedTeam(User manager) {
    return teamRepository
        .findByManagerId(manager.getId())
        .orElseThrow(() -> new NoSuchElementException("You don't manage a team"));
  }

  /** Loads the team and checks the given user is its manager, or throws 403/404. */
  public Team assertManagesTeam(User manager, Long teamId) {
    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new NoSuchElementException("Team not found"));
    if (!team.getManager().getId().equals(manager.getId())) {
      throw new AccessDeniedException("You don't manage this team");
    }
    return team;
  }

  public List<User> listMembers(Team team) {
    return userRepository.findByTeamId(team.getId());
  }

  @Transactional
  public Team updatePolicy(Team team, int minOfficeDaysPerWeek) {
    if (minOfficeDaysPerWeek < 0 || minOfficeDaysPerWeek > 7) {
      throw new IllegalArgumentException("minOfficeDaysPerWeek must be between 0 and 7");
    }
    team.setMinOfficeDaysPerWeek(minOfficeDaysPerWeek);
    return teamRepository.save(team);
  }

  public List<PresenceException> listExceptions(Team team) {
    return exceptionRepository.findByTeamId(team.getId());
  }

  @Transactional
  public PresenceException createException(
      Team team,
      User manager,
      Long userId,
      Integer minOfficeDaysPerWeek,
      LocalDate startDate,
      LocalDate endDate,
      String reason) {

    User member =
        userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
    if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) {
      throw new IllegalArgumentException("That person is not a member of this team");
    }
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate cannot be before startDate");
    }
    if (minOfficeDaysPerWeek != null && (minOfficeDaysPerWeek < 0 || minOfficeDaysPerWeek > 7)) {
      throw new IllegalArgumentException("minOfficeDaysPerWeek must be between 0 and 7");
    }

    PresenceException exception = new PresenceException();
    exception.setTeam(team);
    exception.setUser(member);
    exception.setMinOfficeDaysPerWeek(minOfficeDaysPerWeek);
    exception.setStartDate(startDate);
    exception.setEndDate(endDate);
    exception.setReason(reason);
    exception.setCreatedBy(manager);
    exception.setCreatedAt(Instant.now());
    return exceptionRepository.save(exception);
  }

  @Transactional
  public void deleteException(Team team, Long exceptionId) {
    PresenceException exception =
        exceptionRepository
            .findById(exceptionId)
            .orElseThrow(() -> new NoSuchElementException("Exception not found"));
    if (!exception.getTeam().getId().equals(team.getId())) {
      throw new AccessDeniedException("That exception does not belong to this team");
    }
    exceptionRepository.delete(exception);
  }
}
