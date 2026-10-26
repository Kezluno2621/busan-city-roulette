package com.cityroulette.support;

import com.cityroulette.dto.member.MemberSignupRequest;

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
