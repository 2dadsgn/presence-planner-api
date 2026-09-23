package it.intecs.presenceplanner.web.dto;

import it.intecs.presenceplanner.domain.PresenceType;
import it.intecs.presenceplanner.service.ComplianceService.WeekCompliance;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class TeamDtos {
  private TeamDtos() {}

  public record MemberDto(Long id, String email, String displayName) {}

  public record PolicyDto(@Min(0) @Max(7) int minOfficeDaysPerWeek) {}

  public record MemberMonthDto(
      Long userId,
      String email,
      String displayName,
      Map<LocalDate, PresenceType> assignments,
      List<WeekCompliance> weeks) {}

  public record TeamMonthResponse(
      Long teamId, String teamName, int minOfficeDaysPerWeek, String month, List<MemberMonthDto> members) {}

  public record CreateExceptionRequest(
      @NotNull Long userId,
      /** null = fully exempt for the period */
      @Min(0) @Max(7) Integer minOfficeDaysPerWeek,
      @NotNull LocalDate startDate,
      LocalDate endDate,
      @NotBlank String reason) {}

  public record ExceptionDto(
      Long id,
      Long userId,
      String userDisplayName,
      Integer minOfficeDaysPerWeek,
      LocalDate startDate,
      LocalDate endDate,
      String reason,
      String createdByDisplayName) {}
}
