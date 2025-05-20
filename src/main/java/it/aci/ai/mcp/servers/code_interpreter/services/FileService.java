package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(FileService.class);

    private final StoredFileRepository storedFileRepository;
    private final AppConfig appConfig;

    public FileService(StoredFileRepository storedFileRepository, AppConfig appConfig) {
        this.storedFileRepository = storedFileRepository;
        this.appConfig = appConfig;
    }

    @Transactional
    public StoredFile storeFile(String relativePath, byte[] fileContent, String sessionId,
            StoredFileType storedFileType) {
        String fileId = AppUtils.generateFileId();
        Path storagePath = getStoragePath(sessionId, storedFileType);
        try {
            Files.createDirectories(storagePath);
            // prevent path traversal: normalize and ensure within storagePath
            Path target = storagePath.resolve(relativePath).normalize();
            if (!target.startsWith(storagePath)) {
                throw new IllegalArgumentException("Invalid relative path: " + relativePath);
            }
            Path filePath = Files.write(target, fileContent);
            StoredFile storedFile = new StoredFile(fileId, sessionId, relativePath, Instant.now(),
                    fileContent.length, Files.probeContentType(filePath), storedFileType);
            return storedFileRepository.save(storedFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
        storedFileRepository.delete(storedFile);
        // avoid log injection by sanitizing session id and file id
        String safeSessionId = storedFile.sessionId().replaceAll("[\\r\\n]", "");
        String safeFileId = fileId.replaceAll("[\\r\\n]", "");
        LOG.info("Deleted file - session id: {} - file id: {}", safeSessionId, safeFileId);
    }

    public byte[] downloadFile(String fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId).orElseThrow();
        Path filePath = getFilePath(storedFile);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            // avoid log injection by sanitizing file id and session id
            String safeSessionId = storedFile.sessionId().replaceAll("[\\r\\n]", "");
            String safeFileId = storedFile.id().replaceAll("[\\r\\n]", "");
            LOG.info("Uploaded file - session id: {} - file id: {}", safeSessionId, safeFileId);
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
