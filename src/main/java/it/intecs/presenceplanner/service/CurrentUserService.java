package it.intecs.presenceplanner.service;

import it.intecs.presenceplanner.domain.User;
import it.intecs.presenceplanner.repo.UserRepository;
import it.intecs.presenceplanner.security.AppPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the signed-in caller (mock header or validated Entra ID token,
 * see security package) to a local User row, creating it on first sight
 * ("just-in-time provisioning") so no separate user-management step is
 * needed before someone's first login.
 */
@Service
public class CurrentUserService {

  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public AppPrincipal principal() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof AppPrincipal appPrincipal)) {
      throw new IllegalStateException("No authenticated AppPrincipal on the security context");
    }
    return appPrincipal;
  }

  @Transactional
  public User currentUser() {
    AppPrincipal principal = principal();

    User user =
        userRepository
            .findByEmailIgnoreCase(principal.email())
            .orElseGet(
                () ->
                    userRepository.save(
                        new User(principal.externalId(), principal.email(), principal.displayName())));

    boolean changed = false;
    if (principal.externalId() != null && !principal.externalId().equals(user.getExternalId())) {
      user.setExternalId(principal.externalId());
      changed = true;
    }
    if (!principal.displayName().equals(user.getDisplayName())) {
      user.setDisplayName(principal.displayName());
      changed = true;
    }
    if (changed) {
      user = userRepository.save(user);
    }
    return user;
  }
}
