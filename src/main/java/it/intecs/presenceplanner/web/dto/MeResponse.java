package it.intecs.presenceplanner.web.dto;

public record MeResponse(
    Long id,
    String email,
    String displayName,
    TeamSummary team,
    TeamSummary managedTeam) {

  public record TeamSummary(Long id, String name, int minOfficeDaysPerWeek) {}
}
