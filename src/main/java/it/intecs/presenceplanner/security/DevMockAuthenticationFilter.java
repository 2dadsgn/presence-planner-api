package it.intecs.presenceplanner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Dev-only stand-in for real sign-in. Trusts two headers the way the
 * Angular app's "mock auth" mode fakes a Microsoft sign-in on its side —
 * this is the backend half of that same shortcut, active only while
 * app.auth.mode=mock (see application-dev.yml). Never enabled in the
 * azuread profile.
 *
 * Identifies the caller from:
 *   X-Debug-User-Email: someone@company.com   (required)
 *   X-Debug-User-Name:  Someone Name           (optional, defaults to the email)
 *
 * Falls back to the seeded demo user (daniele.lubrano@intecsengineering.it)
 * when no header is sent, so the API is usable immediately without any
 * client-side wiring.
 */
public class DevMockAuthenticationFilter extends OncePerRequestFilter {

  private static final String DEFAULT_EMAIL = "daniele.lubrano@intecsengineering.it";
  private static final String DEFAULT_NAME = "Daniele Lubrano";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String email = request.getHeader("X-Debug-User-Email");
    String name = request.getHeader("X-Debug-User-Name");

    if (email == null || email.isBlank()) {
      email = DEFAULT_EMAIL;
      name = DEFAULT_NAME;
    } else if (name == null || name.isBlank()) {
      name = email;
    }

    AppPrincipal principal = new AppPrincipal(null, email, name);
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    var authentication =
        new UsernamePasswordAuthenticationToken(principal, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }
}
