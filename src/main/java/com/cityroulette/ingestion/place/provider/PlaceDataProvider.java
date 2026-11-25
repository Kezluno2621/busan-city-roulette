package com.cityroulette.ingestion.place.provider;

import com.cityroulette.ingestion.place.dto.NormalizedProviderPage;

/**
 * 수집 서비스에서 공급자별 API와 응답 형식의 차이를 다룰 수 있도록 연결합니다.
 */
public interface PlaceDataProvider {
    String provider();
    NormalizedProviderPage fetch(int pageNo, int pageSize);
}
