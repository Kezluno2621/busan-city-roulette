package com.cityroulette.domain.place.service;
public class PlaceCoordinatesMissingException extends RuntimeException { public PlaceCoordinatesMissingException(Long id){super("장소에 좌표가 없습니다. id="+id);} }
