package com.cityroulette.service;
public class BusResourceNotFoundException extends RuntimeException { public BusResourceNotFoundException(String type,Long id){super(type+"을(를) 찾을 수 없습니다. id="+id);} }
