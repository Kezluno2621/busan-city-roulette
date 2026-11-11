package com.cityroulette.dto.place;
import com.cityroulette.domain.PlaceSource;
import java.time.LocalDateTime;
public record PlaceSourceView(String provider,String externalId,String sourceUrl,String sourceCategory,LocalDateTime sourceModifiedAt,LocalDateTime lastSyncedAt,String attribution,String contentLicense,String imageLicense,boolean active) {
    public static PlaceSourceView from(PlaceSource s){return new PlaceSourceView(s.getProvider(),s.getExternalId(),s.getSourceUrl(),s.getSourceCategory(),s.getSourceModifiedAt(),s.getLastSyncedAt(),s.getAttribution(),s.getContentLicense(),s.getImageLicense(),s.isActive());}
}
