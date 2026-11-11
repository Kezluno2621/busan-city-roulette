package com.cityroulette.dto.bus;
import java.util.List;
public record BusApiPage<T>(List<T> items,int pageNo,int pageSize,int totalCount) { }
