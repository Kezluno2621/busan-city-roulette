package com.cityroulette.service;

import com.cityroulette.domain.Member;
import com.cityroulette.dto.member.MemberProfile;
import com.cityroulette.dto.member.MemberSignupRequest;
import com.cityroulette.repository.MemberRepository;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                         Validator validator, PlatformTransactionManager transactionManager) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public Member register(MemberSignupRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            var violation = violations.iterator().next();
            throw new RegistrationException(violation.getPropertyPath().toString(), violation.getMessage());
        }
        if (!Objects.equals(request.getPassword(), request.getPasswordConfirm())) {
            throw new RegistrationException("passwordConfirm", "비밀번호가 일치하지 않습니다.");
        }
        // BCrypt는 최대 72바이트까지만 처리하므로 다국어 입력도 바이트 수로 검사합니다.
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegistrationException("password", "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
        }
        checkDuplicates(request);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        try {
            // 저장 실패 시 트랜잭션을 끝낸 뒤 중복 원인을 확인합니다.
            return transactionTemplate.execute(status -> memberRepository.saveAndFlush(new Member(
                    request.getUsername(), encodedPassword, request.getEmail(), request.getDisplayName())));
        } catch (DataIntegrityViolationException exception) {
            // 동시에 가입 요청이 들어온 경우에도 DB unique 제약을 사용자 오류로 바꿉니다.
            checkDuplicates(request);
            throw new RegistrationException(null, "회원가입을 완료하지 못했습니다. 입력값을 확인해주세요.");
        }
    }

    private void checkDuplicates(MemberSignupRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new RegistrationException("username", "이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new RegistrationException("email", "이미 가입된 이메일입니다.");
        }
    }

    @Transactional(readOnly = true)
    public MemberProfile getProfile(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return new MemberProfile(member.getUsername(), member.getDisplayName(), member.getEmail());
    }
}
