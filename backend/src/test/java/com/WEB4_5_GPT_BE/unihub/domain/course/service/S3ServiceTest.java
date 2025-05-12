package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // 테스트용 버킷 이름 설정
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("PDF 파일 업로드 시 S3에 정상 저장되고 URL이 반환되어야 함")
    void test1() throws Exception {
        // given: PDF 헤더를 포함한 더미 바이트
        byte[] pdfBytes = "%PDF-1.4\n%âãÏÓ\n1 0 obj\n<<\n/Type /Catalog\n>>\nendobj\n".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfBytes
        );

        // when
        String url = s3Service.upload(file);

        // then: S3Client.putObject 호출 검증
        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(s3Client).putObject(reqCaptor.capture(), pathCaptor.capture());

        PutObjectRequest actualReq = reqCaptor.getValue();
        Path actualPath = pathCaptor.getValue();

        // bucket 이름 검증
        assertThat(actualReq.bucket()).isEqualTo("test-bucket");

        // key 검증: 타임스탬프_prefix_test.pdf 형태
        String key = actualReq.key();
        assertThat(key).endsWith("_test.pdf");
        assertThat(key).matches("\\d+_test\\.pdf");

        // URL 에 key 포함 및 형식 검증
        assertThat(url).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/");
        assertThat(url).contains(key);

        // 임시 파일 실제 생성 여부
        assertTrue(actualPath.toFile().exists(), "임시 PDF 파일이 생성되어야 합니다");
    }
}
