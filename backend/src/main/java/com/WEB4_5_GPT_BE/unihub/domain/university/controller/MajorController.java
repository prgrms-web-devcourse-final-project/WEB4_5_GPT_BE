package com.WEB4_5_GPT_BE.unihub.domain.university.controller;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.MajorCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.MajorUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    /**
     * 전공 목록 조회 (전체)
     */
    @GetMapping
    public RsData<List<MajorResponse>> getAllMajors() {
        List<MajorResponse> majors = majorService.getAllMajors();
        return new RsData<>("200", "전공 목록 조회에 성공했습니다.", majors);
    }

    /**
     * 대학별 전공 목록 조회 (전체)
     */
    @GetMapping("/university/{universityId}")
    public RsData<List<MajorResponse>> getMajorsByUniversity(
            @PathVariable Long universityId) {
        List<MajorResponse> majors = majorService.getMajorsByUniversity(universityId);
        return new RsData<>("200", "대학별 전공 목록 조회에 성공했습니다.", majors);
    }


    /**
     * 전공 등록
     */
    @PostMapping
    public RsData<MajorResponse> createMajor(@Valid @RequestBody MajorCreateRequest request) {
        MajorResponse major = majorService.createMajor(request);
        return new RsData<>("201", "전공 등록에 성공했습니다.", major);
    }

    /**
     * 전공 정보 수정
     */
    @PutMapping("/{majorId}")
    public RsData<MajorResponse> updateMajor(
            @PathVariable Long majorId,
            @Valid @RequestBody MajorUpdateRequest request) {
        MajorResponse major = majorService.updateMajor(majorId, request);
        return new RsData<>("200", "전공 정보 수정에 성공했습니다.", major);
    }

    /**
     * 전공 삭제
     */
    @DeleteMapping("/{majorId}")
    public RsData<Void> deleteMajor(@PathVariable Long majorId) {
        majorService.deleteMajor(majorId);
        return new RsData<>("200", "전공 삭제에 성공했습니다.");
    }
}
