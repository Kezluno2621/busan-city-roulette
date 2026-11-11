package com.cityroulette.service;

import com.cityroulette.domain.ImportRun;
import com.cityroulette.dto.tourism.TourismCollectionResult;
import com.cityroulette.dto.tourism.TourismImportPageResult;
import com.cityroulette.dto.tourism.TourismPage;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TourismCollectionService {
    private final TourismService tourismService;
    private final TourismPageImporter pageImporter;
    private final ImportRunService runService;

    public TourismCollectionService(TourismService tourismService, TourismPageImporter pageImporter,
                                    ImportRunService runService) {
        this.tourismService = tourismService;
        this.pageImporter = pageImporter;
        this.runService = runService;
    }

    public TourismCollectionResult collect(boolean dryRun, int pageSize, int limit) {
        if (pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("pageSize는 1~100이어야 합니다.");
        if (limit < 1) throw new IllegalArgumentException("limit은 1 이상이어야 합니다.");
        ImportRun run = dryRun ? null : runService.start(false, limit);
        int pages = 0, received = 0, inserted = 0, updated = 0, duplicates = 0, skipped = 0, failed = 0;
        try {
            tourismService.verifyBusanLegalDongCode();
            int pageNo = 1;
            int processed = 0;
            int effectivePageSize = Math.min(pageSize, limit);
            while (processed < limit) {
                TourismPage page = tourismService.getBusanPlacesPage(pageNo, effectivePageSize);
                List<TourismPlaceDto> items = page.items().stream()
                        .limit(limit - processed).toList();
                TourismImportPageResult result = pageImporter.importPage(items, dryRun);
                pages++;
                received += result.received(); inserted += result.inserted(); updated += result.updated();
                duplicates += result.duplicates(); skipped += result.skipped(); failed += result.failed();
                if (run != null) runService.addPage(run.getId(), result);
                processed += items.size();
                if (items.isEmpty() || processed >= page.totalCount()
                        || page.items().size() < effectivePageSize) break;
                pageNo++;
            }
            if (run != null) return runService.succeed(run.getId());
            return new TourismCollectionResult(-1L, "DRY_RUN", true, pages, received, inserted,
                    updated, duplicates, skipped, failed);
        } catch (RuntimeException exception) {
            if (run != null) runService.fail(run.getId(), safeMessage(exception));
            throw exception;
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
