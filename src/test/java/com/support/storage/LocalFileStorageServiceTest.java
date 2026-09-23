package com.support.storage;

import com.support.exception.InvalidOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString());
        storageService.init();
    }

    @Test
    @DisplayName("storeFile & loadFileAsResource — stores and retrieves file correctly")
    void testStoreAndLoadFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", "dummy-image-bytes".getBytes());

        String storageKey = storageService.storeFile(file, "ticket_1");
        assertThat(storageKey).isNotNull().contains("ticket_1").contains("screenshot.png");

        Resource resource = storageService.loadFileAsResource(storageKey);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo("dummy-image-bytes".getBytes());
    }

    @Test
    @DisplayName("storeFile — rejects empty files")
    void testStoreEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> storageService.storeFile(emptyFile, "ticket_1"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot store empty file");
    }

    @Test
    @DisplayName("storeFile — neutralizes path traversal attempts")
    void testPathTraversalProtection() {
        MockMultipartFile attackFile = new MockMultipartFile(
                "file", "../../../etc/passwd", "text/plain", "malicious-data".getBytes());

        // Sanitization ensures file is stored within the allowed root directory without traversing upwards
        String storageKey = storageService.storeFile(attackFile, "ticket_1");
        assertThat(storageKey).isNotNull().startsWith("ticket_1/");
        assertThat(storageKey).doesNotContain("..");
    }

    @Test
    @DisplayName("deleteFile — removes physical file from storage")
    void testDeleteFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "log.txt", "text/plain", "sample-log-content".getBytes());

        String storageKey = storageService.storeFile(file, "ticket_1");
        Resource resource = storageService.loadFileAsResource(storageKey);
        assertThat(resource.exists()).isTrue();

        storageService.deleteFile(storageKey);
        assertThatThrownBy(() -> storageService.loadFileAsResource(storageKey))
                .isNotNull();
    }
}
