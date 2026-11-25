package com.cityroulette.domain.trip.service;

import com.cityroulette.domain.trip.dto.StoredFileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTests {

    @TempDir
    Path tempDirectory;

    @Test
    void storesJpegWithUuidNameAndPreservesOriginalName() throws Exception {
        FileStorageService service = service();
        MockMultipartFile file = file("image", "busan.jpg", "image/jpeg", "jpeg-data".getBytes());

        StoredFileInfo stored = service.store(file).orElseThrow();

        assertThat(stored.originalFileName()).isEqualTo("busan.jpg");
        assertThat(stored.storedFileName())
                .matches("[0-9a-f-]{36}\\.jpg")
                .isNotEqualTo(stored.originalFileName());
        assertThat(Files.readAllBytes(tempDirectory.resolve(stored.storedFileName())))
                .isEqualTo("jpeg-data".getBytes());
    }

    @Test
    void storesPngImage() {
        StoredFileInfo stored = service().store(
                file("image", "busan.png", "image/png", new byte[]{1, 2, 3})).orElseThrow();

        assertThat(stored.storedFileName()).endsWith(".png");
        assertThat(stored.contentType()).isEqualTo("image/png");
    }

    @Test
    void createsDifferentNamesForSameOriginalName() {
        FileStorageService service = service();

        String first = service.store(file("image", "same.webp", "image/webp", new byte[]{1}))
                .orElseThrow().storedFileName();
        String second = service.store(file("image", "same.webp", "image/webp", new byte[]{2}))
                .orElseThrow().storedFileName();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsImageLargerThanFiveMegabytes() {
        byte[] oversized = new byte[(int) FileStorageService.MAX_IMAGE_SIZE + 1];

        assertThatThrownBy(() -> service().store(
                file("image", "large.jpg", "image/jpeg", oversized)))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void rejectsUnsupportedContentTypeAndExtensionMismatch() {
        FileStorageService service = service();

        assertThatThrownBy(() -> service.store(
                file("image", "script.js", "application/javascript", new byte[]{1})))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("이미지");
        assertThatThrownBy(() -> service.store(
                file("image", "fake.exe", "image/jpeg", new byte[]{1})))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("확장자");
    }

    @Test
    void downloadedFileHasSameSha256AsUploadedFile() throws Exception {
        FileStorageService service = service();
        byte[] original = "content".getBytes();
        StoredFileInfo stored = service.store(
                file("image", "load.jpg", "image/jpeg", original)).orElseThrow();
        byte[] downloaded = service.load(stored.storedFileName()).getContentAsByteArray();

        assertThat(downloaded).isEqualTo(original);
        assertThat(MessageDigest.getInstance("SHA-256").digest(downloaded))
                .isEqualTo(MessageDigest.getInstance("SHA-256").digest(original));
    }

    @Test
    void treatsEmptyUploadAsNoAttachment() {
        MockMultipartFile empty = file("image", "", "application/octet-stream", new byte[0]);

        assertThat(service().store(empty)).isEmpty();
    }

    @Test
    void rejectsPathTraversalWhenLoading() {
        assertThatThrownBy(() -> service().load("../secret.jpg"))
                .isInstanceOf(TripRecordNotFoundException.class);
    }

    @Test
    void returnsNotFoundForMissingStoredFile() {
        assertThatThrownBy(() -> service().load(
                "8c51da48-a45d-4fb2-a465-2a7a44946e5d.jpg"))
                .isInstanceOf(TripRecordNotFoundException.class);
    }

    private FileStorageService service() {
        return new FileStorageService(tempDirectory.toString());
    }

    private MockMultipartFile file(String name, String originalName, String contentType, byte[] content) {
        return new MockMultipartFile(name, originalName, contentType, content);
    }
}
