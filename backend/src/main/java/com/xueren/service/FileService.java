package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.config.UploadProperties;
import com.xueren.dto.FileVO;
import com.xueren.entity.StoredFile;
import com.xueren.repository.StoredFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private final StoredFileRepository storedFileRepository;
    private final Path uploadDir;

    public FileService(StoredFileRepository storedFileRepository, UploadProperties uploadProperties) {
        this.storedFileRepository = storedFileRepository;
        this.uploadDir = Paths.get(uploadProperties.getDir()).toAbsolutePath();
    }

    public FileVO upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        try {
            Files.createDirectories(uploadDir);
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String suffix = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
            String storedName = UUID.randomUUID() + suffix;
            Path target = uploadDir.resolve(storedName);
            file.transferTo(target);

            StoredFile storedFile = new StoredFile();
            storedFile.setUploaderId(userId);
            storedFile.setOriginalName(originalName);
            storedFile.setStoredPath("/uploads/" + storedName);
            storedFile.setFileSize(file.getSize());
            storedFile.setMimeType(file.getContentType());
            storedFileRepository.save(storedFile);

            return FileVO.builder()
                    .id(storedFile.getId())
                    .originalName(storedFile.getOriginalName())
                    .url(storedFile.getStoredPath())
                    .fileSize(storedFile.getFileSize())
                    .mimeType(storedFile.getMimeType())
                    .build();
        } catch (IOException ex) {
            throw new BusinessException("文件上传失败");
        }
    }
}
