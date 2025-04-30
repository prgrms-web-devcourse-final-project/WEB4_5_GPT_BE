package com.WEB4_5_GPT_BE.unihub.domain.university.controller;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.UniversityRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

  /** 대학 단건 조회 */
  @GetMapping("/{universityId}")
  public RsData<UniversityResponse> getUniversity(@PathVariable Long universityId) {
    UniversityResponse university = universityService.getUniversity(universityId);
    return new RsData<>("200", "대학 조회에 성공했습니다.", university);
  }

    /**
     * 대학 등록
     */
    @PostMapping
    public RsData<UniversityResponse> createUniversity(
            @Valid @RequestBody UniversityRequest request) {
        UniversityResponse university = universityService.createUniversity(request);
        return new RsData<>("201", "대학 등록에 성공했습니다.", university);
    }

    /**
     * 대학 정보 수정
     */
    @PutMapping("/{universityId}")
    public RsData<UniversityResponse> updateUniversity(
            @PathVariable Long universityId, @Valid @RequestBody UniversityRequest request) {
        UniversityResponse university = universityService.updateUniversity(universityId, request);
        return new RsData<>("200", "대학 정보 수정에 성공했습니다.", university);
  }

  /** 대학 삭제 */
  @DeleteMapping("/{universityId}")
  public RsData<Void> deleteUniversity(@PathVariable Long universityId) {
    universityService.deleteUniversity(universityId);
    return new RsData<>("200", "대학 삭제에 성공했습니다.");
  }
}
