package com.scar.bookvault.media.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MediaService {
    private final MinioClient minioClient;
    private final String bucketName;

    public MediaService(MinioClient minioClient, @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        initializeBucket();
    }

    private void initializeBucket() {
        try {
            boolean found = minioClient.bucketExists(io.minio.BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
            if (!found) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            }
        } catch (Exception e) {
            System.err.println("Error initializing bucket: " + e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            String contentType = file.getContentType();

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build()
            );

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public InputStream getFile(String fileName) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    public String getFileUrl(String fileName) {
        return "/api/media/v1/files/" + fileName;
    }
}

