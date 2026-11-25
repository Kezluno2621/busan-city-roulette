package com.cityroulette.ingestion.place;

import com.cityroulette.ingestion.place.dto.PlaceDataQualityReport;
import com.cityroulette.ingestion.place.dto.PlaceDataQualityReport.ProviderQuality;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceDataQualityService {
    private final JdbcTemplate jdbc;

    public PlaceDataQualityService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public PlaceDataQualityReport measure() {
        Counts counts = jdbc.queryForObject("""
                SELECT COUNT(*),
                       COALESCE(SUM(CASE WHEN latitude IS NULL OR longitude IS NULL THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN address IS NULL OR TRIM(address) = '' THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN image_url IS NULL OR TRIM(image_url) = '' THEN 1 ELSE 0 END), 0)
                FROM places
                """, (rs, row) -> new Counts(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
        long mergeCandidates = jdbc.queryForObject("SELECT COUNT(*) FROM place_merge_candidates", Long.class);
        List<ProviderQuality> providers = jdbc.query("""
                SELECT provider, COUNT(*), COUNT(DISTINCT place_id),
                       COALESCE(SUM(CASE WHEN image_license IS NULL OR TRIM(image_license) = '' THEN 1 ELSE 0 END), 0)
                FROM place_sources GROUP BY provider ORDER BY provider
                """, (rs, row) -> new ProviderQuality(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
        return new PlaceDataQualityReport(LocalDateTime.now(), counts.total, counts.coordinates,
                rate(counts.coordinates, counts.total), counts.addresses, rate(counts.addresses, counts.total),
                counts.images, rate(counts.images, counts.total), mergeCandidates,
                distribution("category"), distribution("district"), List.copyOf(providers));
    }

    private Map<String, Long> distribution(String column) {
        if (!("category".equals(column) || "district".equals(column))) throw new IllegalArgumentException("unsupported column");
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query("SELECT " + column + ", COUNT(*) FROM places GROUP BY " + column + " ORDER BY " + column,
                (rs, row) -> Map.entry(rs.getString(1) == null || rs.getString(1).isBlank()
                        ? "(미확인)" : rs.getString(1), rs.getLong(2)))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(result);
    }

    private double rate(long missing, long total) {
        return total == 0 ? 0.0 : Math.round(missing * 10_000.0 / total) / 100.0;
    }

    private record Counts(long total, long coordinates, long addresses, long images) { }
}
