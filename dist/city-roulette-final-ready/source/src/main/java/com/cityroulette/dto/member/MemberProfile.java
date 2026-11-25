package com.cityroulette.dto.member;

// 화면에는 비밀번호 해시를 포함하지 않는 데이터만 전달합니다.
public record MemberProfile(String username, String displayName, String email) {
}
