package com.cityroulette.service;

import com.cityroulette.domain.Member;
import com.cityroulette.domain.Role;
import com.cityroulette.dto.member.MemberSignupRequest;
import com.cityroulette.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.cityroulette.support.MemberTestData.PASSWORD;
import static com.cityroulette.support.MemberTestData.signup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTests {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void registersUserWithBcryptPassword() {
        Member member = memberService.register(signup("serviceuser", "service@example.com"));
        Member stored = memberRepository.findByUsername("serviceuser").orElseThrow();

        assertThat(member.getId()).isNotNull();
        assertThat(stored.getRole()).isEqualTo(Role.USER);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateUsername() {
        memberService.register(signup("serviceuser", "first@example.com"));

        assertThatThrownBy(() -> memberService.register(signup("serviceuser", "second@example.com")))
                .hasMessage("이미 사용 중인 아이디입니다.");
        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateEmail() {
        memberService.register(signup("firstuser", "service@example.com"));

        assertThatThrownBy(() -> memberService.register(signup("seconduser", "service@example.com")))
                .hasMessage("이미 가입된 이메일입니다.");
        assertThat(memberRepository.existsByUsername("seconduser")).isFalse();
    }

    @Test
    void rejectsPasswordConfirmationMismatch() {
        MemberSignupRequest request = signup("serviceuser", "service@example.com");
        request.setPasswordConfirm("different-value");

        assertThatThrownBy(() -> memberService.register(request))
                .hasMessageContaining("비밀번호");
        assertThat(memberRepository.existsByUsername("serviceuser")).isFalse();
    }

    @Test
    void rejectsPasswordExceedingBcryptByteLimit() {
        MemberSignupRequest request = signup("serviceuser", "service@example.com");
        String longPassword = "가".repeat(25);
        request.setPassword(longPassword);
        request.setPasswordConfirm(longPassword);

        assertThatThrownBy(() -> memberService.register(request))
                .hasMessageContaining("72바이트");
        assertThat(memberRepository.existsByUsername("serviceuser")).isFalse();
    }

    @Test
    void loadsDatabaseMemberAsSecurityUser() {
        Member member = memberService.register(signup("securityuser", "security@example.com"));
        var user = userDetailsService.loadUserByUsername("securityuser");

        assertThat(user.getUsername()).isEqualTo(member.getUsername());
        assertThat(passwordEncoder.matches(PASSWORD, user.getPassword())).isTrue();
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missinguser"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
