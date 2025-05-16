package com.WEB4_5_GPT_BE.unihub.global.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * AWS S3에 관련된 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    // 활성 프로파일 (예: "dev", "prod")
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * MultipartFile을 S3에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     *
     * 업로드된 파일은 S3에 저장되며, URL을 통해 접근할 수 있습니다.
     * S3에 퍼블릭 읽기 권한을 부여하고 고정적인 URL을 부여받기 위해 버킷 정책(Bucket Policy)이 설정되어 있어야 합니다.
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     * @throws IOException 파일 처리 중 발생하는 예외
     */
    public String upload(MultipartFile file) throws IOException {

        // 1) 한글 인코딩 처리 및 공백을 "_"(언더바)로 대체
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");                                  // "자료구조 강의계획서.png"
        String replacedSpaces = filename.replaceAll("\\s+", "_"); // "자료구조_강의계획서.png"
        String encoded = URLEncoder.encode(replacedSpaces, StandardCharsets.UTF_8);

        // 2) 파일을 임시 디렉토리에 저장
        Path filePath = Files.createTempFile("up-", "-" + encoded);
        file.transferTo(filePath.toFile());

        // 3) S3에 업로드할 키 생성
        String timestamp = String.valueOf(System.currentTimeMillis());
        String key = "%s/%s-%s".formatted(activeProfile, timestamp, replacedSpaces); // ex) dev/1684092000000-자료구조_강의계획서.png

        // 4) S3에 파일 업로드 (KEY, FILE)
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).build(),
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
