package it.intecs.presenceplanner.domain;

import jakarta.persistence.*;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The Microsoft Entra ID (Azure AD) object id ("oid" claim). Null until the user signs in for real (dev/mock mode). */
  @Column(name = "external_id", unique = true)
  private String externalId;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  /** The team this user belongs to as a member, if any. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  public User(String externalId, String email, String displayName) {
    this.externalId = externalId;
    // Stored lower-cased so a plain (non-expression) unique index on this
    // column is enough to enforce case-insensitive uniqueness in the DB —
    // H2's CREATE INDEX doesn't support expressions like lower(email), so
    // relying on a functional index there isn't an option.
    this.email = email == null ? null : email.toLowerCase(Locale.ROOT);
    this.displayName = displayName;
  }
}
