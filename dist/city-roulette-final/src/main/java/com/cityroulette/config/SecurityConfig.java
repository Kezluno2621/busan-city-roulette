package com.cityroulette.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        .requestMatchers(HttpMethod.GET, "/dev/api-status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tourism/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/generate").permitAll()
                        .requestMatchers("/api/courses/**").authenticated()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/", "/places", "/places/**", "/recommend", "/recommend/search-location",
                                "/signup", "/login", "/error",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/api/places/**", "/api/tourism/**", "/api/transit/**", "/api/bus/**",
                                "/api/recommendations/**").permitAll()
                        .requestMatchers("/my", "/my/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(login -> login.loginPage("/login")
                        .defaultSuccessUrl("/", false).failureUrl("/login?error").permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
        // DB를 변경하는 관광지 동기화 API도 관리자 권한과 CSRF 토큰을 모두 요구합니다.
        return http.build();
    }
}
