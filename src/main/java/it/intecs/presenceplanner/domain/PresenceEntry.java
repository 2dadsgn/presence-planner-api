package it.intecs.presenceplanner.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One user's assigned value for one calendar day. */
@Entity
@Table(
    name = "presence_entry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entry_date"}))
@Getter
@Setter
@NoArgsConstructor
public class PresenceEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "entry_date", nullable = false)
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private PresenceType type;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public PresenceEntry(User user, LocalDate date, PresenceType type) {
    this.user = user;
    this.date = date;
    this.type = type;
  }
}
