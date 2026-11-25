package com.cityroulette.domain.place.dto;
import com.cityroulette.domain.place.entity.PlaceSource;
import java.time.LocalDateTime;
public record PlaceSourceView(String provider,String externalId,String sourceUrl,String sourceCategory,LocalDateTime sourceModifiedAt,LocalDateTime lastSyncedAt,String attribution,String contentLicense,String imageLicense,boolean active) {
    public static PlaceSourceView from(PlaceSource s){return new PlaceSourceView(s.getProvider(),s.getExternalId(),s.getSourceUrl(),s.getSourceCategory(),s.getSourceModifiedAt(),s.getLastSyncedAt(),s.getAttribution(),s.getContentLicense(),s.getImageLicense(),s.isActive());}
}
