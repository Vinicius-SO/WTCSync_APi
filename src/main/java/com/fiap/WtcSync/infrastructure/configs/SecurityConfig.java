package com.fiap.WtcSync.infrastructure.configs;

import com.fiap.WtcSync.application.services.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/index.html")
            .permitAll()
            .requestMatchers("/api/auth/login", "/api/auth/google", "/api/auth/register").permitAll()
            .requestMatchers("/api/health", "/api/deeplinks/routes").permitAll()
            .requestMatchers("/api/auth/me", "/api/auth/firebase-token", "/api/users/me/fcm-token").authenticated()
            .requestMatchers("/ws/support").hasAnyAuthority("CLIENT", "OPERATOR", "ATENDENTE")
            .requestMatchers("/api/support/queue", "/api/support/*/claim", "/api/support/*/release").hasAnyAuthority("OPERATOR", "ATENDENTE")
            .requestMatchers("/api/support/*").hasAnyAuthority("CLIENT", "OPERATOR", "ATENDENTE")
            .requestMatchers("/api/clients/me").hasAuthority("CLIENT")
            .requestMatchers("/messages/**", "/inbox/**", "/conversation").hasAnyAuthority("CLIENT", "OPERATOR", "ATENDENTE")
            .requestMatchers("/api/clients/**").hasAnyAuthority("OPERATOR", "ATENDENTE")
            .requestMatchers("/api/campaigns/**", "/api/segments/**", "/api/audit/**", "/api/health/errors").hasAuthority("OPERATOR")
            .anyRequest().denyAll())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}