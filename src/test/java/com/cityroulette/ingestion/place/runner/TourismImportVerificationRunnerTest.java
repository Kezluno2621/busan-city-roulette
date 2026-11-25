package com.cityroulette.ingestion.place.runner;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TourismImportVerificationRunnerTest {
    @Test void blocksWriteWhenAnyIsolationProofIsMissing() {
        var failures = TourismImportVerificationRunner.safetyFailures(false, "isolated", "original", "10",
                "EXPECTED", "ACTUAL", "ACTUAL", false, false);
        assertThat(failures).contains("isolated-database-confirmed=false", "database mismatch",
                "Flyway V12 required", "isolated JDBC URL hash mismatch",
                "original JDBC URL separation not proven", "backup file missing");
    }

    @Test void allowsOnlyFullyProvenV12BackupBackedIsolation() {
        assertThat(TourismImportVerificationRunner.safetyFailures(true, "isolated", "isolated", "12",
                "ISOLATED_HASH", "ISOLATED_HASH", "ORIGINAL_HASH", true, true)).isEmpty();
    }
}
