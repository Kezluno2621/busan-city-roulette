package com.cityroulette.global.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** 공개 경로와 회원·관리자 전용 경로를 구분하고 폼 로그인을 설정합니다. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 넓은 경로 패턴보다 관리자 전용 규칙과 공개 코스 생성 예외를 먼저 평가합니다.
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
        return http.build();
    }
}
