package it.intecs.presenceplanner.security;

import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns a validated Microsoft Entra ID access token into an AppPrincipal,
 * so the rest of the app never touches raw JWT claims.
 *
 * Entra ID puts the user's email in "preferred_username" (or "upn" for
 * some tenant configurations) and their display name in "name"; "oid" is
 * their stable per-tenant object id, used as our externalId.
 */
public class AzureAdJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String email = firstNonBlank(jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("upn"));
    String name = firstNonBlank(jwt.getClaimAsString("name"), email);
    String oid = jwt.getClaimAsString("oid");

    AppPrincipal principal = new AppPrincipal(oid, email, name);
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    return new AzureAdAuthenticationToken(principal, jwt, authorities);
  }

  private static String firstNonBlank(String a, String b) {
    return (a != null && !a.isBlank()) ? a : b;
  }
}
