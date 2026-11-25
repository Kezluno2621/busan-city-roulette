package com.cityroulette.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 데이터 수집 실행 이력의 저장 구조를 표현하는 JPA 엔티티입니다. 식별자와 컬럼 매핑을 정의합니다.
 */
@Entity
@Table(name = "import_runs")
public class ImportRun {
    public enum Status { RUNNING, SUCCESS, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50) private String provider;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "dry_run", nullable = false) private boolean dryRun;
    @Column(name = "record_limit") private Integer recordLimit;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "finished_at") private LocalDateTime finishedAt;
    @Column(name = "pages_requested", nullable = false) private int pagesRequested;
    @Column(name = "received_count", nullable = false) private int receivedCount;
    @Column(name = "inserted_count", nullable = false) private int insertedCount;
    @Column(name = "updated_count", nullable = false) private int updatedCount;
    @Column(name = "duplicate_count", nullable = false) private int duplicateCount;
    @Column(name = "skipped_count", nullable = false) private int skippedCount;
    @Column(name = "failed_count", nullable = false) private int failedCount;
    @Column(name = "error_message", length = 1000) private String errorMessage;

    protected ImportRun() { }

    public ImportRun(String provider, boolean dryRun, Integer recordLimit) {
        this.provider = provider;
        this.status = Status.RUNNING.name();
        this.dryRun = dryRun;
        this.recordLimit = recordLimit;
        this.startedAt = LocalDateTime.now();
    }

    public void addPage(int received, int inserted, int updated, int duplicates, int skipped, int failed) {
        pagesRequested++;
        receivedCount += received;
        insertedCount += inserted;
        updatedCount += updated;
        duplicateCount += duplicates;
        skippedCount += skipped;
        failedCount += failed;
    }

    public void succeed() { status = Status.SUCCESS.name(); finishedAt = LocalDateTime.now(); }
    public void fail(String message) {
        status = Status.FAILED.name();
        failedCount++;
        errorMessage = message == null ? null : message.substring(0, Math.min(message.length(), 1000));
        finishedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public boolean isDryRun() { return dryRun; }
    public Integer getRecordLimit() { return recordLimit; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public int getPagesRequested() { return pagesRequested; }
    public int getReceivedCount() { return receivedCount; }
    public int getInsertedCount() { return insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public int getSkippedCount() { return skippedCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
}
