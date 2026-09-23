package com.support.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * ==============================================================================================
 * STORAGE ABSTRACTION: FileStorageService
 * ==============================================================================================
 * 
 * Provides an enterprise storage contract decoupled from underlying infrastructure.
 * Implementations can target the local filesystem (LocalFileStorageService) or cloud object stores
 * (AWS S3, MinIO, Google Cloud Storage, Azure Blob Storage) without altering domain services.
 */
public interface FileStorageService {

    /**
     * Persists an uploaded multipart file and returns an immutable storage key.
     *
     * @param file the incoming multipart payload
     * @param subDirectory optional logical partition (e.g. ticket ID folder)
     * @return unique storage key for later retrieval
     */
    String storeFile(MultipartFile file, String subDirectory);

    /**
     * Loads the physical file as a Spring Resource suitable for HTTP streaming.
     *
     * @param storageKey the unique key returned by storeFile
     * @return readable Resource byte stream
     */
    Resource loadFileAsResource(String storageKey);

    /**
     * Deletes the physical file from storage.
     *
     * @param storageKey the unique key returned by storeFile
     */
    void deleteFile(String storageKey);
}
