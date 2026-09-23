package com.support.storage;

import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

/**
 * ==============================================================================================
 * STORAGE IMPLEMENTATION: LocalFileStorageService
 * ==============================================================================================
 * 
 * WHY THIS STORAGE SECURITY DESIGN:
 * 1. Path Traversal Neutralization (OWASP ASVS §12.3):
 *    - Replaces unsafe characters in user filenames.
 *    - Validates that the resolved absolute path starts strictly with the configured `storageBaseDir`.
 *      Any sequence containing `../`, `..\\`, or absolute roots immediately throws an exception.
 * 
 * 2. Cryptographic Storage Key Isolation:
 *    - Physical storage uses `UUID + "_" + sanitizedName` so that files from different tickets or users
 *      never collide, even if two customers upload an image with the exact same name `screenshot.png`.
 * 
 * 3. Graceful Initialization:
 *    - Pre-creates the root upload directory on application startup (`@PostConstruct`) if not yet present.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.storage.local.base-dir:uploads/attachments}") String baseDir) {
        this.rootLocation = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Local attachment storage initialized at: {}", this.rootLocation);
        } catch (IOException ex) {
            log.error("Could not initialize local attachment storage at: {}", this.rootLocation, ex);
            throw new InvalidOperationException("Could not initialize storage folder: " + ex.getMessage());
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Cannot store empty file.");
        }

        String rawFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        // Strip any directory path provided in filename (e.g. from unix or windows clients)
        String cleanedName = StringUtils.getFilename(StringUtils.cleanPath(rawFilename));
        if (cleanedName == null || cleanedName.isBlank()) {
            cleanedName = "attachment";
        }
        
        // Remove path traversal double dots and sanitize to safe alphanumeric and permitted characters
        String sanitizedFilename = cleanedName.replace("..", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
        // Ensure no multiple consecutive dots remain
        sanitizedFilename = sanitizedFilename.replaceAll("\\.{2,}", "_");
        if (sanitizedFilename.isBlank() || sanitizedFilename.equals(".") || sanitizedFilename.equals("_")) {
            sanitizedFilename = "attachment_" + System.currentTimeMillis();
        }

        // Generate unique UUID-prefixed storage key
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFilename;
        String relativePath = (subDirectory != null && !subDirectory.isBlank()) 
                ? subDirectory.trim() + "/" + uniqueFileName 
                : uniqueFileName;

        try {
            Path targetLocation = this.rootLocation.resolve(relativePath).normalize();

            // Security check: ensure target path is within rootLocation
            if (!targetLocation.startsWith(this.rootLocation)) {
                log.warn("Path traversal attack detected! Attempted path: {}", relativePath);
                throw new InvalidOperationException("Cannot store file outside current storage directory.");
            }

            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.debug("Stored attachment: {} -> {}", rawFilename, targetLocation);
            return relativePath;
        } catch (IOException ex) {
            log.error("Failed to store file {}", rawFilename, ex);
            throw new InvalidOperationException("Failed to store file: " + ex.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String storageKey) {
        try {
            Path filePath = this.rootLocation.resolve(storageKey).normalize();

            // Security check: ensure target path is within rootLocation
            if (!filePath.startsWith(this.rootLocation)) {
                log.warn("Path traversal attack detected on load! StorageKey: {}", storageKey);
                throw new InvalidOperationException("Cannot access file outside current storage directory.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File", "key", storageKey);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File", "key", storageKey);
        }
    }

    @Override
    public void deleteFile(String storageKey) {
        try {
            Path filePath = this.rootLocation.resolve(storageKey).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
                log.debug("Deleted physical file: {}", filePath);
            }
        } catch (IOException ex) {
            log.warn("Failed to delete file for storageKey: {}", storageKey, ex);
        }
    }
}
