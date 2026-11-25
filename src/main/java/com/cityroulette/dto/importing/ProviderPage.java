package com.cityroulette.dto.importing;

import java.util.List;

/**
 * 공급자 조회의 한 페이지에서 받은 원천 항목과 페이지 진행 정보를 수집 서비스로 전달합니다.
 */
public record ProviderPage<T>(List<T> items, int pageNo, int pageSize, int totalCount) { }
