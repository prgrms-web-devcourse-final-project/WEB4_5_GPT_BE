package com.WEB4_5_GPT_BE.unihub.domain.home.controller;

import com.WEB4_5_GPT_BE.unihub.domain.home.service.S3Service;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3Service s3Service;

    /**
     * POST /api/file/upload
     * - public-read ACL 로 S3 업로드
     * - 정적 URL 반환
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<String> upload(@RequestParam("file") MultipartFile file) throws Exception {

        String url = s3Service.upload(file);

        return new RsData<>("200", "파일 업로드 성공", url);
    }

}
