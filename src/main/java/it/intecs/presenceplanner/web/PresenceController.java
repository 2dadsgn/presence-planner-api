package it.intecs.presenceplanner.web;

import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.service.CurrentUserService;
import it.intecs.presenceplanner.service.PresenceService;
import it.intecs.presenceplanner.web.dto.PresenceDtos.AssignRequest;
import it.intecs.presenceplanner.web.dto.PresenceDtos.ClearRequest;
import it.intecs.presenceplanner.web.dto.PresenceDtos.MonthResponse;
import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.web.bind.annotation.*;

/** The signed-in user's own presence calendar. */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

  private final PresenceService presenceService;
  private final CurrentUserService currentUserService;

  public PresenceController(PresenceService presenceService, CurrentUserService currentUserService) {
    this.presenceService = presenceService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public MonthResponse getMonth(@RequestParam int year, @RequestParam int month) {
    User user = currentUserService.currentUser();
    YearMonth ym = YearMonth.of(year, month);
    return new MonthResponse(ym.toString(), presenceService.getMonth(user, ym));
  }

  @PostMapping("/assign")
  public MonthResponse assign(@Valid @RequestBody AssignRequest request) {
    User user = currentUserService.currentUser();
    presenceService.assign(user, request.dates(), request.type());
    YearMonth ym = YearMonth.from(request.dates().get(0));
    return new MonthResponse(ym.toString(), presenceService.getMonth(user, ym));
  }

  @PostMapping("/clear")
  public MonthResponse clear(@Valid @RequestBody ClearRequest request) {
    User user = currentUserService.currentUser();
    presenceService.clear(user, request.dates());
    YearMonth ym = YearMonth.from(request.dates().get(0));
    return new MonthResponse(ym.toString(), presenceService.getMonth(user, ym));
  }
}
