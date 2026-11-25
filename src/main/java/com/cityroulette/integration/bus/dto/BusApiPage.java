package com.cityroulette.integration.bus.dto;
import java.util.List;
public record BusApiPage<T>(List<T> items,int pageNo,int pageSize,int totalCount) { }
