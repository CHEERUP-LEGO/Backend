package com.team9.jobbotdari.service;

import com.team9.jobbotdari.entity.File;
import com.team9.jobbotdari.entity.User;
import com.team9.jobbotdari.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket-name}") // 추가: S3 버킷 이름 설정값 주입
    private String bucketName;

    @Value("${cloud.aws.s3.base-url}")
    private String baseUrl;

    private final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif"); // 허용할 확장자 목록

    public void saveFile(MultipartFile multipartFile, User user) {
        updateFile(multipartFile, user);
    }

    public void updateFile(MultipartFile multipartFile, User user) {
        if (multipartFile.isEmpty()) {
            return;
        }

        try {
            String originalFilename = multipartFile.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new IllegalArgumentException("허용되지 않은 파일 형식입니다. (지원 형식: jpg, jpeg, png, gif)");
            }

            // S3에 저장할 고유 파일 이름 생성
            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
            String s3key = "uploads/" + uniqueFilename;

            // S3에 파일 업로드
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3key)
                            .contentType(multipartFile.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(multipartFile.getBytes())
            );


            // 기존 파일 삭제 후 새로운 파일 저장
            fileRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).ifPresent(existingFile -> {
                deleteFileFromS3(existingFile.getFilePath());
                fileRepository.delete(existingFile);
            });

            File savedFile = File.builder()
                    .user(user)
                    .filename(originalFilename)
                    .filePath(s3key)
                    .build();

            fileRepository.save(savedFile);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류 발생", e);
        }
    }

    // S3에서 파일 삭제
    public void deleteFileFromS3(String s3key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3key)
                    .build());
        } catch (Exception e) {
            log.error("S3에서 파일 삭제 중 오류 발생: {}", e.getMessage());
        }
    }

    // 파일 확장자 추출 메서드
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return ""; // 확장자가 없거나 잘못된 경우
        }
        return filename.substring(dotIndex + 1);
    }
}