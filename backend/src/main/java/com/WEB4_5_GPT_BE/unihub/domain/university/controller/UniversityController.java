package com.WEB4_5_GPT_BE.unihub.domain.university.controller;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "University", description = "대학교 정보 관리 API (대학 목록 및 상세 정보 조회)")
@RestController
@RequestMapping("/api/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @Operation(summary = "대학 목록 조회", description = "전체 대학교 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대학 목록 조회 성공"),
    })
    @GetMapping
    public RsData<List<UniversityResponse>> getAllUniversities() {
        List<UniversityResponse> universities = universityService.getAllUniversities();
        return new RsData<>("200", "대학 목록 조회에 성공했습니다.", universities);
    }

    @Operation(summary = "대학 상세 조회", description = "특정 대학교의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대학 상세 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 대학교를 찾을 수 없음")
    })
    @GetMapping("/{universityId}")
    public RsData<UniversityResponse> getUniversity(@PathVariable Long universityId) {
        UniversityResponse university = universityService.getUniversity(universityId);
        return new RsData<>("200", "대학 조회에 성공했습니다.", university);
    }


}
