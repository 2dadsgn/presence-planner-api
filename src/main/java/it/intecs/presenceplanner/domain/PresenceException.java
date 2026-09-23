package it.intecs.presenceplanner.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A per-person override of a team's minimum-office-days policy, set by the
 * team's manager. A null {@link #minOfficeDaysPerWeek} means the person is
 * fully exempt for the covered period; otherwise it replaces the team's
 * default minimum for that person during the period.
 */
@Entity
@Table(name = "presence_exception")
@Getter
@Setter
@NoArgsConstructor
public class PresenceException {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Null = fully exempt from the minimum during this period. */
  @Column(name = "min_office_days_per_week")
  private Integer minOfficeDaysPerWeek;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  /** Null = open-ended (applies indefinitely from startDate). */
  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(nullable = false)
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public boolean coversWeek(LocalDate weekStart, LocalDate weekEnd) {
    boolean startsBeforeOrOnWeekEnd = !startDate.isAfter(weekEnd);
    boolean endsAfterOrOnWeekStart = endDate == null || !endDate.isBefore(weekStart);
    return startsBeforeOrOnWeekEnd && endsAfterOrOnWeekStart;
  }
}
