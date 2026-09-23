package it.intecs.presenceplanner.security;

/**
 * The identity extracted from whichever auth mechanism is active (mock
 * header auth in dev, a validated Microsoft Entra ID JWT in the azuread
 * profile). Everything downstream (CurrentUserService, controllers) works
 * against this one shape, so it never needs to know which mode is active.
 */
public record AppPrincipal(String externalId, String email, String displayName) {}
