package it.intecs.presenceplanner.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the JwtDecoder used in azuread mode with an added audience check:
 * Spring Boot's issuer-uri auto-config validates signature/issuer/expiry
 * on its own, but NOT that the token was actually issued for this API
 * (vs. e.g. Microsoft Graph, or another app in the same tenant) — that
 * needs its own validator, wired in here.
 */
@Configuration
@ConditionalOnProperty(value = "app.auth.mode", havingValue = "azuread")
public class AzureAdJwtDecoderConfig {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Value("${azure-ad.api-audience}")
  private String apiAudience;

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder =
        (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

    OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> withIssuer =
        JwtValidators.createDefaultWithIssuer(issuerUri);
    OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> withAudience =
        new JwtClaimValidator<java.util.List<String>>(
            "aud", audiences -> audiences != null && audiences.contains(apiAudience));

    decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
        withIssuer, withAudience));

    return decoder;
  }
}
