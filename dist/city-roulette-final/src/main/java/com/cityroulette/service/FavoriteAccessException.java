package com.cityroulette.service;

public class FavoriteAccessException extends RuntimeException {
    public FavoriteAccessException() { super("회원 정보를 확인할 수 없습니다."); }
}
