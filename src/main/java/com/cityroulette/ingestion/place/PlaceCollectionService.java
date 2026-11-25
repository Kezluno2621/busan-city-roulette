package com.cityroulette.ingestion.place;

import com.cityroulette.ingestion.ImportRunService;
import com.cityroulette.ingestion.ImportRun;
import com.cityroulette.ingestion.place.dto.PlaceImportResult;
import com.cityroulette.ingestion.place.dto.ProviderInspection;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlaceCollectionService {
    private final Map<String, PlaceDataProvider> providers = new LinkedHashMap<>();
    private final PlaceImportService importer;
    private final ImportRunService runs;

    public PlaceCollectionService(List<PlaceDataProvider> providers, PlaceImportService importer, ImportRunService runs) {
        providers.forEach(provider -> this.providers.put(provider.provider(), provider));
        this.importer = importer; this.runs = runs;
    }

    public List<String> providerNames() { return List.copyOf(providers.keySet()); }

    public ProviderInspection inspect(String requestedProvider, int pageSize) {
        String name = requestedProvider.toUpperCase(Locale.ROOT).trim();
        PlaceDataProvider provider = providers.get(name);
        if (provider == null) throw new IllegalArgumentException("지원하지 않는 provider: " + requestedProvider);
        if (pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("pageSize는 1~100이어야 합니다.");
        var page = provider.fetch(1, pageSize);
        return new ProviderInspection(name, page.totalCount(), page.items().size(), page.mappingFailures());
    }

    public PlaceImportResult collect(String requestedProvider, boolean dryRun, int pageSize, int limit) {
        String name = requestedProvider.toUpperCase(Locale.ROOT).trim();
        PlaceDataProvider provider = providers.get(name);
        if (provider == null) throw new IllegalArgumentException("지원하지 않는 provider: " + requestedProvider);
        if (pageSize < 1 || pageSize > 100 || limit < 1) throw new IllegalArgumentException("pageSize는 1~100, limit은 1 이상이어야 합니다.");
        ImportRun run = dryRun ? null : runs.start(name, limit);
        PlaceImportResult total = PlaceImportResult.empty(name, dryRun);
        try {
            int pageNo=1, processed=0;
            while (processed < limit) {
                int remaining = limit - processed;
                var page=provider.fetch(pageNo,pageSize);
                var items=page.items().stream().limit(limit-processed).toList();
                int pageFailures = Math.min(page.mappingFailures(), Math.max(0, remaining - items.size()));
                PlaceImportResult pageResult=importer.importItems(name,items,dryRun);
                total=total.add(pageResult.fetched(),pageResult.inserted(),pageResult.updated(),pageResult.duplicates(),
                        pageResult.skipped(),pageResult.failed()+pageFailures);
                if (run != null) runs.addPage(run.getId(), pageResult.fetched(), pageResult.inserted(), pageResult.updated(),
                        pageResult.duplicates(), pageResult.skipped(), pageResult.failed()+pageFailures);
                int receivedOnPage = page.items().size() + page.mappingFailures();
                processed += Math.min(remaining, receivedOnPage);
                if (receivedOnPage == 0 || pageNo * pageSize >= page.totalCount() || receivedOnPage < pageSize) break;
                pageNo++;
            }
            if (run != null) runs.succeedOnly(run.getId());
            return total;
        } catch (RuntimeException exception) {
            if (run != null) runs.fail(run.getId(), safeMessage(exception));
            throw exception;
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message=exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
