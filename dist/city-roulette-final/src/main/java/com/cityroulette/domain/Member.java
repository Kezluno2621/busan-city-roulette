package com.cityroulette.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_members_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_members_email", columnNames = "email")
})
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String username;
    @Column(nullable = false, length = 60)
    private String password;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, length = 30)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Member() {
        // JPA용 기본 생성자입니다.
    }

    public Member(String username, String password, String email, String displayName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.displayName = displayName;
        this.role = Role.USER;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
