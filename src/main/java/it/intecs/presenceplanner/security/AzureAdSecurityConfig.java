package it.intecs.presenceplanner.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Real Microsoft/Entra ID authentication: validates the access token the
 * Angular app (via MSAL) attaches to each request, against the tenant's
 * issuer. Active only when app.auth.mode=azuread — see
 * application-azuread.yml for the issuer-uri / audience you need to set
 * once IT has registered the API's app registration.
 */
@Configuration
@ConditionalOnProperty(value = "app.auth.mode", havingValue = "azuread")
public class AzureAdSecurityConfig {

  @Bean
  public SecurityFilterChain azureAdFilterChain(
      HttpSecurity http,
      // See DevSecurityConfig.devFilterChain for why this needs @Qualifier.
      @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsSource))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated())
        // Our converter fully replaces the principal (-> AppPrincipal) and its authorities;
        // a "roles"/"groups" claim -> Spring authority mapping would plug in here too.
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(
            jwt -> jwt.jwtAuthenticationConverter(new AzureAdJwtAuthenticationConverter())));

    return http.build();
  }
}
