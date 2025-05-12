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
import it.aci.ai.mcp.servers.code_interpreter.exception.CodeInterpreterException;
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

    // Self-injection for proper transactional behavior on internal calls
    @org.springframework.beans.factory.annotation.Autowired
    private FileService self;

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
            // Prevent path traversal by normalizing and validating the target path
            Path target = storagePath.resolve(relativePath).normalize();
            if (!target.startsWith(storagePath)) {
                throw new CodeInterpreterException("Invalid file path: " + relativePath);
            }
            // Ensure parent directories exist
            Files.createDirectories(target.getParent());
            Path filePath = Files.write(target, fileContent);
            StoredFile storedFile = new StoredFile(fileId, sessionId, relativePath, Instant.now(),
                    fileContent.length, Files.probeContentType(filePath), storedFileType);
            return storedFileRepository.save(storedFile);
        } catch (IOException e) {
            throw new CodeInterpreterException("Failed to store file '" + relativePath + "' for session '" + sessionId + "'", e);
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
            throw new CodeInterpreterException("Failed to delete file with id '" + fileId + "'", e);
        }
        storedFileRepository.delete(storedFile);
        // Log deletion without exposing user-controlled identifiers
        LOG.info("Deleted file");
    }

    public byte[] downloadFile(String fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId).orElseThrow();
        Path filePath = getFilePath(storedFile);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new CodeInterpreterException("Failed to read file with id '" + fileId + "'", e);
        }
    }

    @Transactional
    public List<StoredFile> uploadFile(List<UploadedFile> uploadedFiles) {
        String sessionId = AppUtils.generateSessionId();
        List<StoredFile> storedFiles = new ArrayList<>();
        for (UploadedFile uploadedFile : uploadedFiles) {
            String filename = uploadedFile.name();
            byte[] fileContent = uploadedFile.content();
            // Use injected self to ensure @Transactional on storeFile is applied
            StoredFile storedFile = self.storeFile(filename, fileContent, sessionId, StoredFileType.INPUT);
            storedFiles.add(storedFile);
            LOG.info("Uploaded file - session id: {} - file id: {}", storedFile.sessionId(), storedFile.id());
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
