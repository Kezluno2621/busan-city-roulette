package com.cityroulette.service;

import com.cityroulette.dto.trip.StoredFileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    public static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private static final Pattern STORED_FILE_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        if (uploadDirectory == null || uploadDirectory.isBlank()) {
            throw new FileStorageException("파일 저장 경로가 설정되지 않았습니다.");
        }
        this.uploadRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException exception) {
            throw new FileStorageException("파일 저장 폴더를 만들 수 없습니다.", exception);
        }
    }

    /** 첨부파일을 검증·저장합니다. 첨부가 생략되면 빈 값을 반환합니다. */
    public Optional<StoredFileInfo> store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new FileValidationException("파일 크기는 5MB 이하만 가능합니다.");
        }

        String contentType = normalizeContentType(file.getContentType());
        String storedExtension = CONTENT_TYPE_EXTENSIONS.get(contentType);
        if (storedExtension == null) {
            throw new FileValidationException("JPG, PNG, WEBP 이미지 파일만 업로드할 수 있습니다.");
        }

        String originalFileName = validateOriginalFileName(file.getOriginalFilename(), contentType);
        String storedFileName = UUID.randomUUID() + storedExtension;
        Path target = resolveStoredFile(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target);
        } catch (IOException exception) {
            deleteQuietly(storedFileName);
            throw new FileStorageException("이미지 파일을 저장하지 못했습니다.", exception);
        }

        return Optional.of(new StoredFileInfo(
                originalFileName, storedFileName, contentType, file.getSize()));
    }

    public Resource load(String storedFileName) {
        Path storedPath = resolveStoredFile(storedFileName);
        Resource resource = new FileSystemResource(storedPath);
        if (!resource.exists() || !resource.isReadable() || !Files.isRegularFile(storedPath)) {
            throw new TripRecordNotFoundException();
        }
        return resource;
    }

    public void delete(String storedFileName) {
        Path storedPath = resolveStoredFile(storedFileName);
        try {
            Files.deleteIfExists(storedPath);
        } catch (IOException exception) {
            throw new FileStorageException("저장된 이미지 파일을 정리하지 못했습니다.", exception);
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String validateOriginalFileName(String rawFileName, String contentType) {
        String fileName = StringUtils.cleanPath(rawFileName == null ? "" : rawFileName.trim());
        if (fileName.isBlank() || fileName.contains("..")
                || fileName.contains("/") || fileName.contains("\\")) {
            throw new FileValidationException("파일 이름이 올바르지 않습니다.");
        }

        String lowerName = fileName.toLowerCase(Locale.ROOT);
        boolean extensionMatches = switch (contentType) {
            case "image/jpeg" -> lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
            case "image/png" -> lowerName.endsWith(".png");
            case "image/webp" -> lowerName.endsWith(".webp");
            default -> false;
        };
        if (!extensionMatches) {
            throw new FileValidationException("파일 확장자와 이미지 형식을 확인해주세요.");
        }
        return fileName;
    }

    private Path resolveStoredFile(String storedFileName) {
        String normalizedName = storedFileName == null
                ? "" : storedFileName.toLowerCase(Locale.ROOT);
        if (!STORED_FILE_PATTERN.matcher(normalizedName).matches()) {
            throw new TripRecordNotFoundException();
        }
        Path path = uploadRoot.resolve(normalizedName).normalize();
        if (!path.getParent().equals(uploadRoot)) {
            throw new TripRecordNotFoundException();
        }
        return path;
    }

    private void deleteQuietly(String storedFileName) {
        try {
            Files.deleteIfExists(resolveStoredFile(storedFileName));
        } catch (IOException | RuntimeException ignored) {
            // 원래 저장 실패를 유지하고 정리 실패는 추가로 전파하지 않습니다.
        }
    }
}
