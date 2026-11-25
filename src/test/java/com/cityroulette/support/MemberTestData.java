package com.cityroulette.support;

import com.cityroulette.dto.member.MemberSignupRequest;

/**
 * 회원 테스트에서 공통으로 사용할 테스트용 데이터를 제공합니다.
 */
public final class MemberTestData {

    public static final String PASSWORD = "test-pass-1234";

    private MemberTestData() {
    }

    public static MemberSignupRequest signup(String username, String email) {
        MemberSignupRequest request = new MemberSignupRequest();
        request.setUsername(username);
        request.setPassword(PASSWORD);
        request.setPasswordConfirm(PASSWORD);
        request.setEmail(email);
        request.setDisplayName("테스트유저");
        return request;
    }
}
