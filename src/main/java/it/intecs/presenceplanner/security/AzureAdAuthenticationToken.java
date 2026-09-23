package it.intecs.presenceplanner.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** An authenticated request in azuread mode: principal is our AppPrincipal, credentials the raw Jwt. */
public class AzureAdAuthenticationToken extends AbstractAuthenticationToken {

  private final AppPrincipal principal;
  private final Jwt jwt;

  public AzureAdAuthenticationToken(
      AppPrincipal principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.jwt = jwt;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return jwt;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }
}
