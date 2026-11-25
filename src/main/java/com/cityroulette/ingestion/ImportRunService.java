package com.cityroulette.ingestion;

import com.cityroulette.ingestion.tourapi.dto.TourismCollectionResult;
import com.cityroulette.ingestion.tourapi.dto.TourismImportPageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportRunService {
    private final ImportRunRepository repository;
    public ImportRunService(ImportRunRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportRun start(boolean dryRun, Integer limit) {
        return repository.save(new ImportRun("TOUR_API", dryRun, limit));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportRun start(String provider, Integer limit) {
        return repository.save(new ImportRun(provider, false, limit));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addPage(long id, int received, int inserted, int updated, int duplicates, int skipped, int failed) {
        ImportRun run = repository.findById(id).orElseThrow();
        run.addPage(received, inserted, updated, duplicates, skipped, failed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeedOnly(long id) {
        ImportRun run = repository.findById(id).orElseThrow();
        run.succeed();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addPage(long id, TourismImportPageResult result) {
        ImportRun run = repository.findById(id).orElseThrow();
        run.addPage(result.received(), result.inserted(), result.updated(), result.duplicates(),
                result.skipped(), result.failed());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TourismCollectionResult succeed(long id) {
        ImportRun run = repository.findById(id).orElseThrow();
        run.succeed();
        return resultOf(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(long id, String message) {
        ImportRun run = repository.findById(id).orElseThrow();
        run.fail(message);
    }

    private TourismCollectionResult resultOf(ImportRun run) {
        return new TourismCollectionResult(run.getId(), run.getStatus(), run.isDryRun(),
                run.getPagesRequested(), run.getReceivedCount(), run.getInsertedCount(),
                run.getUpdatedCount(), run.getDuplicateCount(), run.getSkippedCount(),
                run.getFailedCount());
    }
}
