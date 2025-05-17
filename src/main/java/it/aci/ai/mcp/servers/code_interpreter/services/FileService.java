package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.dto.UploadedFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFileType;
import it.aci.ai.mcp.servers.code_interpreter.repositories.StoredFileRepository;
import it.aci.ai.mcp.servers.code_interpreter.utils.AppUtils;

@Service
public class FileService {


    private final StoredFileRepository storedFileRepository;
    private final AppConfig appConfig;

    public FileService(StoredFileRepository storedFileRepository, AppConfig appConfig) {
        this.storedFileRepository = storedFileRepository;
        this.appConfig = appConfig;
    }

    // Remove transactional annotation to allow repository tx in calling methods
    public StoredFile storeFile(String relativePath, byte[] fileContent, String sessionId,
            StoredFileType storedFileType) {
        // Prevent path traversal
        Path rel = Path.of(relativePath).normalize();
        if (rel.isAbsolute() || rel.startsWith("..")) {
            throw new IllegalArgumentException("Invalid file path: " + relativePath);
        }
        String fileId = AppUtils.generateFileId();
        Path storageDir = getStoragePath(sessionId, storedFileType);
        try {
            Path filePath = storageDir.resolve(rel);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileContent);
            StoredFile storedFile = new StoredFile(fileId, sessionId, rel.toString(), Instant.now(),
                    fileContent.length, Files.probeContentType(filePath), storedFileType);
            return storedFileRepository.save(storedFile);
        } catch (IOException e) {
            throw new it.aci.ai.mcp.servers.code_interpreter.exception.FileStorageException(
                    "Failed to store file '" + relativePath + "' for session '" + sessionId + "'", e);
        }
    }

    public Path getFilePath(StoredFile storedFile) {
        Path basePath = getStoragePath(storedFile.sessionId(), storedFile.type());
        return basePath.resolve(storedFile.relativePath());
    }

    @Transactional
    public void deleteFile(String fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId).orElseThrow();
        try {
            Files.delete(getFilePath(storedFile));
        } catch (IOException e) {
            throw new it.aci.ai.mcp.servers.code_interpreter.exception.FileStorageException(
                    "Failed to delete file '" + fileId + "'", e);
        }
        storedFileRepository.delete(storedFile);
        // deletion acknowledged (logging removed to avoid potential info leakage)
    }

    public byte[] downloadFile(String fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId).orElseThrow();
        Path filePath = getFilePath(storedFile);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new it.aci.ai.mcp.servers.code_interpreter.exception.FileStorageException(
                    "Failed to download file '" + fileId + "'", e);
        }
    }

    @Transactional
    public List<StoredFile> uploadFile(List<UploadedFile> uploadedFiles) {
        String sessionId = AppUtils.generateSessionId();
        List<StoredFile> storedFiles = new ArrayList<>();
        for (UploadedFile uploadedFile : uploadedFiles) {
            String filename = uploadedFile.name();
            byte[] fileContent = uploadedFile.content();
            StoredFile storedFile = storeFile(filename, fileContent, sessionId, StoredFileType.INPUT);
            storedFiles.add(storedFile);
            // upload acknowledged (logging removed to avoid potential info leakage)
        }
        return storedFiles;
    }

    public List<StoredFile> findSessionFiles(String sessionId) {
        return storedFileRepository.findBySessionId(sessionId);
    }

    public List<StoredFile> findUploadedFiles(String sessionId) {
        return storedFileRepository.findBySessionIdAndType(sessionId, StoredFileType.INPUT);
    }

    private Path getStoragePath(String sessionId, StoredFileType storedFileType) {
        Path sessionStoragePath = appConfig.getStoragePath().resolve(sessionId);
        String folderName = switch (storedFileType) {
            case INPUT:
                yield "upload";
            case OUTPUT:
                yield "output";
        };
        return sessionStoragePath.resolve(folderName);
    }

}
