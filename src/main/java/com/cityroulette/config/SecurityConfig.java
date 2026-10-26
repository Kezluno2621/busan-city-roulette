package com.cityroulette.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/places", "/recommend", "/signup", "/login", "/error",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/api/places/**", "/api/tourism/**", "/api/transit/**",
                                "/api/recommendations/**").permitAll()
                        .requestMatchers("/my", "/my/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(login -> login.loginPage("/login")
                        .defaultSuccessUrl("/", false).failureUrl("/login?error").permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
        // CSRF 기본 보호를 유지합니다. 현재 JSON POST API는 구현되어 있지 않습니다.
        return http.build();
    }
}
