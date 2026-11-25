package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedPlace;

/**
 * 외부 또는 상위 계층의 값을 내부에서 사용하는 형식으로 변환합니다.
 */
public interface ProviderPlaceMapper<T> {
    NormalizedPlace map(T source);
}
