package com.WEB4_5_GPT_BE.unihub.domain.university.controller;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    /**
     * 대학 목록 조회 (전체)
     */
    @GetMapping
    public RsData<List<UniversityResponse>> getAllUniversities() {
        List<UniversityResponse> universities = universityService.getAllUniversities();
        return new RsData<>("200", "대학 목록 조회에 성공했습니다.", universities);
    }

    /**
     * 대학 단건 조회
     */
    @GetMapping("/{universityId}")
    public RsData<UniversityResponse> getUniversity(@PathVariable Long universityId) {
        UniversityResponse university = universityService.getUniversity(universityId);
        return new RsData<>("200", "대학 조회에 성공했습니다.", university);
    }


}
