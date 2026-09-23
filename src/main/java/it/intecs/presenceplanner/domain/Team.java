package it.intecs.presenceplanner.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A team owns one presence policy (minimum office days per week) that its
 * manager can configure, plus per-person exceptions to it.
 */
@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  /** The manager who owns this team's policy and can view its members' presence. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "manager_id", nullable = false)
  private User manager;

  @Column(name = "min_office_days_per_week", nullable = false)
  private int minOfficeDaysPerWeek;

  public Team(String name, User manager, int minOfficeDaysPerWeek) {
    this.name = name;
    this.manager = manager;
    this.minOfficeDaysPerWeek = minOfficeDaysPerWeek;
  }
}
