package com.cityroulette.repository;

import com.cityroulette.domain.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static com.cityroulette.support.MemberTestData.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class MemberRepositoryTests {

    private static final String ENCODED_PASSWORD = new BCryptPasswordEncoder().encode(PASSWORD);

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsMemberAndChecksExistence() {
        Member member = memberRepository.saveAndFlush(new Member(
                "repositoryuser", ENCODED_PASSWORD, "repository@example.com", "테스트유저"));
        entityManager.clear();

        Member found = memberRepository.findByUsername("repositoryuser").orElseThrow();
        assertThat(found.getId()).isEqualTo(member.getId());
        assertThat(found.getEmail()).isEqualTo("repository@example.com");
        assertThat(found.getDisplayName()).isEqualTo("테스트유저");
        assertThat(found.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(memberRepository.existsByUsername("repositoryuser")).isTrue();
        assertThat(memberRepository.existsByUsername("missinguser")).isFalse();
        assertThat(memberRepository.existsByEmail("repository@example.com")).isTrue();
        assertThat(memberRepository.existsByEmail("missing@example.com")).isFalse();
        assertThat(memberRepository.findByUsername("missinguser")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email"})
    void databaseRejectsDuplicateUsernameOrEmail(String field) {
        memberRepository.saveAndFlush(new Member("firstuser", ENCODED_PASSWORD,
                "first@example.com", "테스트유저"));
        Member duplicate = new Member(field.equals("username") ? "firstuser" : "seconduser",
                ENCODED_PASSWORD, field.equals("email") ? "first@example.com" : "second@example.com",
                "다른유저");

        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
