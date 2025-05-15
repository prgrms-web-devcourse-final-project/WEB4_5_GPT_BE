package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
        ReflectionTestUtils.setField(s3Service, "activeProfile", "test");
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

        // 1) bucket 이름 검증
        assertThat(actualReq.bucket()).isEqualTo("test-bucket");

        // 2) 키 검증: "dev/타임스탬프-test.pdf" 형태
        String key = actualReq.key();
        assertThat(key).startsWith("test/");
        assertThat(key).endsWith("-test.pdf");
        // 전체 패턴 매칭 (dev/1234567890123-test.pdf)
        assertThat(key).matches("test/\\d+-test\\.pdf");

        // 3) URL 에 key 포함 및 형식 검증
        assertThat(url).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/");
        assertThat(url).contains("/" + key);

        // 4) 임시 파일 실제 생성 여부
        assertTrue(actualPath.toFile().exists(), "임시 PDF 파일이 생성되어야 합니다");
    }

    @Test
    @DisplayName("deleteByKey 호출 시 S3Client.deleteObject에 올바른 요청 전달")
    void test2_deleteByKey_invokesDeleteWithCorrectKey() {
        // given
        String key = "1612345678900_plan.pdf";

        // when
        s3Service.deleteByKey(key);

        // then
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest deleteReq = captor.getValue();
        assertThat(deleteReq.bucket()).isEqualTo("test-bucket");
        assertThat(deleteReq.key()).isEqualTo(key);
    }

    @Test
    @DisplayName("deleteByUrl 호출 시 URL에서 key를 추출하여 S3Client.deleteObject에 올바른 요청 전달")
    void test3_deleteByUrl_extractsKeyAndInvokesDelete() {
        // given
        String key = "1612345678900_plan.pdf";
        String url = String.format("https://test-bucket.s3.ap-northeast-2.amazonaws.com/%s", key);

        // when
        s3Service.deleteByUrl(url);

        // then
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest deleteReq = captor.getValue();
        assertThat(deleteReq.bucket()).isEqualTo("test-bucket");
        assertThat(deleteReq.key()).isEqualTo(key);
    }
}
