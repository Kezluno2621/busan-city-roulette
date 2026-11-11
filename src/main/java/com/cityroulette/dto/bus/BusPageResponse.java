package com.cityroulette.dto.bus;
import java.util.List;
public record BusPageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages) { }
