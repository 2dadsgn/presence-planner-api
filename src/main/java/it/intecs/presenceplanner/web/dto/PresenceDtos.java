package it.intecs.presenceplanner.web.dto;

import it.intecs.presenceplanner.domain.PresenceType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class PresenceDtos {
  private PresenceDtos() {}

  public record MonthResponse(String month, Map<LocalDate, PresenceType> assignments) {}

  public record AssignRequest(
      @NotEmpty List<LocalDate> dates, @NotNull PresenceType type) {}

  public record ClearRequest(@NotEmpty List<LocalDate> dates) {}
}
