package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AWS S3에 관련된 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * MultipartFile을 S3에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     * <p>
     * 업로드된 파일은 S3에 저장되며, URL을 통해 접근할 수 있습니다.
     * S3에 퍼블릭 읽기 권한을 부여하고 고정적인 URL을 부여받기 위해 버킷 정책(Bucket Policy)이 설정되어 있어야 합니다.
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     * @throws IOException 파일 처리 중 발생하는 예외
     */
    public String upload(MultipartFile file) throws IOException {

        // 1) 임시 파일 생성
        Path filePath = Files.createTempFile("up-", "-" + file.getOriginalFilename());
        file.transferTo(filePath.toFile());

        // 2) S3에 저장할 키 생성
        String key = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // S3에 파일 업로드 (KEY, FILE)
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                filePath
        );

        // 3) 정적 URL 생성
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, key);
    }

    /**
     * S3에 저장된 객체를 key로 삭제합니다.
     *
     * @param key S3 버킷 내 객체 키 (예: "1612345678900_plan.pdf")
     */
    public void deleteByKey(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
        );
    }

    /**
     * 업로드 후 반환된 URL을 이용해 S3 객체를 삭제합니다.
     *
     * @param fileUrl upload() 이 반환한 URL
     */
    public void deleteByUrl(String fileUrl) {
        // URL 의 path 부분에서 "/" 제거 후 key 추출
        URI uri = URI.create(fileUrl);
        String path = uri.getPath();            // ex: "/1612345678900_plan.pdf"
        String key = path.startsWith("/")
                ? path.substring(1)
                : path; // ex: "1612345678900_plan.pdf"
        deleteByKey(key);
    }
}
