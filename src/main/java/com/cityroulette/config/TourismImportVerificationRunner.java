package com.cityroulette.config;

import com.cityroulette.dto.importing.PlaceImportResult;
import com.cityroulette.repository.*;
import com.cityroulette.service.PlaceCollectionService;
import com.cityroulette.service.PlaceDataQualityService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.Connection;
import java.util.*;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "tourism.verification.enabled", havingValue = "true")
public class TourismImportVerificationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TourismImportVerificationRunner.class);
    private static final List<String> ORDERED_PROVIDERS = List.of("TOUR_API", "BUSAN_ATTRACTION",
            "BUSAN_RESTAURANT", "BUSAN_FOODIE", "BUSAN_GALMAETGIL");

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final ExternalApiProperties apiKeys;
    private final PlaceCollectionService collection;
    private final PlaceDataQualityService quality;
    private final PlaceRepository places;
    private final PlaceSourceRepository sources;
    private final PlaceMergeCandidateRepository candidates;
    private final ImportRunRepository runs;
    private final Mode mode;
    private final List<String> providers;
    private final int pageSize;
    private final int limit;
    private final boolean expandedLimitEnabled;
    private final String expectedDatabase;
    private final String expectedUrlHash;
    private final String originalUrlHash;
    private final boolean isolatedConfirmed;
    private final Path backupFile;
    private final String backupHash;

    public TourismImportVerificationRunner(DataSource dataSource, JdbcTemplate jdbc, ExternalApiProperties apiKeys,
            PlaceCollectionService collection, PlaceDataQualityService quality, PlaceRepository places,
            PlaceSourceRepository sources, PlaceMergeCandidateRepository candidates, ImportRunRepository runs,
            @Value("${tourism.verification.mode:INSPECT}") String mode,
            @Value("${tourism.verification.providers:ALL}") String providers,
            @Value("${tourism.verification.page-size:20}") int pageSize,
            @Value("${tourism.verification.limit:20}") int limit,
            @Value("${tourism.verification.expanded-limit-enabled:false}") boolean expandedLimitEnabled,
            @Value("${tourism.verification.expected-database:}") String expectedDatabase,
            @Value("${tourism.verification.expected-jdbc-url-sha256:}") String expectedUrlHash,
            @Value("${tourism.verification.original-jdbc-url-sha256:}") String originalUrlHash,
            @Value("${tourism.verification.isolated-database-confirmed:false}") boolean isolatedConfirmed,
            @Value("${tourism.verification.backup-file:}") String backupFile,
            @Value("${tourism.verification.backup-sha256:}") String backupHash) {
        this.dataSource = dataSource; this.jdbc = jdbc; this.apiKeys = apiKeys; this.collection = collection;
        this.quality = quality; this.places = places; this.sources = sources; this.candidates = candidates;
        this.runs = runs; this.mode = Mode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        this.providers = parseProviders(providers); this.pageSize = pageSize; this.limit = limit;
        this.expandedLimitEnabled = expandedLimitEnabled;
        this.expectedDatabase = expectedDatabase.trim(); this.expectedUrlHash = normalizeHash(expectedUrlHash);
        this.originalUrlHash = normalizeHash(originalUrlHash); this.isolatedConfirmed = isolatedConfirmed;
        this.backupFile = backupFile.isBlank() ? null : Path.of(backupFile).toAbsolutePath().normalize();
        this.backupHash = normalizeHash(backupHash);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int maximum = expandedLimitEnabled ? 100 : 20;
        if (pageSize < 1 || pageSize > maximum || limit < 1 || limit > maximum) {
            throw new IllegalArgumentException("검증 수집 pageSize와 limit은 1~" + maximum + "이어야 합니다.");
        }
        Inspection inspection = inspect();
        logInspection(inspection);
        if (mode == Mode.INSPECT) return;

        requireConfiguredKeys();
        if (expandedLimitEnabled) logProviderScopes();
        Map<String, PlaceImportResult> dryResults = collect(true);
        requireSuccessfulDryRun(dryResults);
        logResults("DRY_RUN", dryResults);
        if (mode == Mode.DRY_RUN) return;

        verifyWriteSafety(inspection);
        Map<Long, String> originalSnapshot = placeSnapshot();
        Counts before = counts();
        Map<String, PlaceImportResult> first = collect(false);
        Counts afterFirst = counts();
        Map<Long, String> firstSnapshot = placeSnapshot();
        Map<String, PlaceImportResult> second = collect(false);
        Counts afterSecond = counts();
        Map<Long, String> secondSnapshot = placeSnapshot();

        boolean originalIdsPreserved = secondSnapshot.keySet().containsAll(originalSnapshot.keySet());
        long originalRowsChanged = originalSnapshot.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getValue(), secondSnapshot.get(entry.getKey()))).count();
        boolean stable = afterFirst.sameDataCounts(afterSecond) && firstSnapshot.equals(secondSnapshot);
        logResults("FIRST_WRITE", first); logResults("SECOND_WRITE", second);
        log.info("TOURISM_VERIFY_COUNTS before={} afterFirst={} afterSecond={}", before, afterFirst, afterSecond);
        log.info("TOURISM_VERIFY_ASSERTIONS originalIdsPreserved={} originalRowsChanged={} reimportStable={}",
                originalIdsPreserved, originalRowsChanged, stable);
        log.info("TOURISM_VERIFY_QUALITY {}", quality.measure());
        if (!originalIdsPreserved || !stable) {
            throw new IllegalStateException("저장/재수집 무결성 검증에 실패했습니다.");
        }
    }

    private void logProviderScopes() {
        providers.forEach(provider -> {
            var inspection = collection.inspect(provider, Math.min(pageSize, 20));
            log.info("TOURISM_VERIFY_SCOPE provider={} reportedTotalCount={} sampleCount={} mappingFailures={} pageNumbering=ONE_BASED",
                    inspection.provider(), inspection.reportedTotalCount(), inspection.sampleCount(), inspection.mappingFailures());
        });
    }

    private Inspection inspect() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getCatalog();
            String urlHash = sha256(connection.getMetaData().getURL().getBytes(StandardCharsets.UTF_8));
            String flyway = jdbc.query("SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1",
                    rs -> rs.next() ? rs.getString(1) : null);
            return new Inspection(database, urlHash, flyway, counts(), keyStatus());
        }
    }

    private void logInspection(Inspection inspection) {
        log.info("TOURISM_VERIFY_INSPECT mode={} database={} jdbcUrlSha256={} flywayVersion={} counts={} keys={}",
                mode, inspection.database, inspection.urlHash, inspection.flyway, inspection.counts, inspection.keys);
    }

    private Map<String, Boolean> keyStatus() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("TOUR_API", apiKeys.getTour().isConfigured());
        result.put("BUSAN_ATTRACTION", apiKeys.getBusan().getAttraction().isConfigured());
        result.put("BUSAN_RESTAURANT", apiKeys.getBusan().getRestaurant().isConfigured());
        result.put("BUSAN_FOODIE", apiKeys.getBusan().getFoodie().isConfigured());
        result.put("BUSAN_GALMAETGIL", apiKeys.getBusan().getGalmaetgil().isConfigured());
        return Collections.unmodifiableMap(result);
    }

    private void requireConfiguredKeys() {
        Map<String, Boolean> status = keyStatus();
        List<String> missing = providers.stream().filter(provider -> !Boolean.TRUE.equals(status.get(provider))).toList();
        if (!missing.isEmpty()) throw new IllegalStateException("요청한 공급자의 API 키가 설정되지 않았습니다: " + missing);
    }

    private Map<String, PlaceImportResult> collect(boolean dryRun) {
        Map<String, PlaceImportResult> results = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        for (String provider : providers) {
            try { results.put(provider, collection.collect(provider, dryRun, pageSize, limit)); }
            catch (RuntimeException exception) { failures.add(provider + ":" + exception.getClass().getSimpleName()); }
        }
        if (!failures.isEmpty()) throw new IllegalStateException("공급자 수집 실패: " + failures);
        return results;
    }

    private void requireSuccessfulDryRun(Map<String, PlaceImportResult> results) {
        List<String> invalid = results.values().stream()
                .filter(result -> result.fetched() < 1 || result.failed() > 0 || result.fetched() > limit)
                .map(PlaceImportResult::provider).toList();
        if (!invalid.isEmpty()) throw new IllegalStateException("Dry-run 품질 조건을 충족하지 못했습니다: " + invalid);
    }

    private void verifyWriteSafety(Inspection inspection) throws IOException {
        boolean backupExists = backupFile != null && Files.isRegularFile(backupFile);
        boolean backupMatches = backupExists && !backupHash.isBlank() && backupHash.equals(sha256(backupFile));
        List<String> failures = safetyFailures(isolatedConfirmed, expectedDatabase, inspection.database,
                inspection.flyway, expectedUrlHash, inspection.urlHash, originalUrlHash, backupExists, backupMatches);
        if (!failures.isEmpty()) throw new IllegalStateException("WRITE_VERIFY 안전조건 불충족: " + failures);
    }

    static List<String> safetyFailures(boolean isolatedConfirmed, String expectedDatabase, String actualDatabase,
            String flyway, String expectedUrlHash, String actualUrlHash, String originalUrlHash,
            boolean backupExists, boolean backupMatches) {
        List<String> failures = new ArrayList<>();
        if (!isolatedConfirmed) failures.add("isolated-database-confirmed=false");
        if (expectedDatabase.isBlank() || !expectedDatabase.equals(actualDatabase)) failures.add("database mismatch");
        if (!"12".equals(flyway)) failures.add("Flyway V12 required");
        if (expectedUrlHash.isBlank() || !expectedUrlHash.equals(actualUrlHash)) failures.add("isolated JDBC URL hash mismatch");
        if (originalUrlHash.isBlank() || originalUrlHash.equals(actualUrlHash)) failures.add("original JDBC URL separation not proven");
        if (!backupExists) failures.add("backup file missing");
        else if (!backupMatches) failures.add("backup SHA-256 mismatch");
        return List.copyOf(failures);
    }

    private Counts counts() {
        return new Counts(places.count(), sources.count(), candidates.count(), runs.count());
    }

    private Map<Long, String> placeSnapshot() {
        Map<Long, String> snapshot = new LinkedHashMap<>();
        jdbc.query("SELECT id,name,category,address,latitude,longitude FROM places ORDER BY id", rs -> {
            String row = String.join("|", Objects.toString(rs.getString(2), ""), Objects.toString(rs.getString(3), ""),
                    Objects.toString(rs.getString(4), ""), Objects.toString(rs.getObject(5), ""),
                    Objects.toString(rs.getObject(6), ""));
            snapshot.put(rs.getLong(1), sha256(row.getBytes(StandardCharsets.UTF_8)));
        });
        return snapshot;
    }

    private void logResults(String stage, Map<String, PlaceImportResult> results) {
        results.values().forEach(result -> log.info(
                "TOURISM_VERIFY_RESULT stage={} provider={} fetched={} inserted={} updated={} duplicates={} skipped={} failed={}",
                stage, result.provider(), result.fetched(), result.inserted(), result.updated(), result.duplicates(),
                result.skipped(), result.failed()));
    }

    private List<String> parseProviders(String value) {
        if ("ALL".equalsIgnoreCase(value.trim())) return ORDERED_PROVIDERS;
        List<String> parsed = Arrays.stream(value.split(",")).map(String::trim)
                .map(x -> x.toUpperCase(Locale.ROOT)).filter(x -> !x.isBlank()).toList();
        if (parsed.isEmpty() || !ORDERED_PROVIDERS.containsAll(parsed)) throw new IllegalArgumentException("지원하지 않는 공급자 목록입니다.");
        return List.copyOf(new LinkedHashSet<>(parsed));
    }

    private String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = digest(); byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
        }
    }
    private String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)).toUpperCase(Locale.ROOT); }
    private MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private String normalizeHash(String value) { return value == null ? "" : value.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT); }

    enum Mode { INSPECT, DRY_RUN, WRITE_VERIFY }
    record Inspection(String database, String urlHash, String flyway, Counts counts, Map<String, Boolean> keys) { }
    record Counts(long places, long sources, long mergeCandidates, long importRuns) {
        boolean sameDataCounts(Counts other) { return places == other.places && sources == other.sources && mergeCandidates == other.mergeCandidates; }
    }
}
