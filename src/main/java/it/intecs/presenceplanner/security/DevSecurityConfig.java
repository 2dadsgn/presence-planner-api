package it.intecs.presenceplanner.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Active only when app.auth.mode=mock (the default "dev" profile, see
 * application-dev.yml). Trades real Microsoft sign-in for the header-based
 * DevMockAuthenticationFilter so the API is runnable before Entra ID is
 * configured. Switch to the "azuread" profile for real auth — see
 * AzureAdSecurityConfig.
 */
@Configuration
@ConditionalOnProperty(value = "app.auth.mode", havingValue = "mock")
public class DevSecurityConfig {

  @Bean
  public SecurityFilterChain devFilterChain(
      HttpSecurity http,
      // Explicit @Qualifier because Spring MVC's own autoconfigured
      // HandlerMappingIntrospector bean also implements CorsConfigurationSource,
      // so this parameter type alone is ambiguous between it and our own
      // corsConfigurationSource bean (CorsConfig) — regardless of whether the
      // compiler retains parameter names (-parameters flag).
      @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsSource))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(
            org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
            .anyRequest().authenticated())
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // needed for H2 console
        .addFilterBefore(new DevMockAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
