package com.WEB4_5_GPT_BE.unihub.domain.university.controller;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Major", description = "전공 정보 관리 API (전공 목록 및 대학별 전공 조회)")
@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
public class MajorController {

  private final MajorService majorService;

  @Operation(summary = "전공 목록 조회", description = "전체 전공 목록을 조회합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "전공 목록 조회 성공")
  })
  @GetMapping
  public RsData<List<MajorResponse>> getAllMajors() {
    List<MajorResponse> majors = majorService.getAllMajors();
    return new RsData<>("200", "전공 목록 조회에 성공했습니다.", majors);
  }

  @Operation(summary = "대학별 전공 목록 조회", description = "특정 대학의 전공 목록을 조회합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "대학별 전공 목록 조회 성공"),
          @ApiResponse(responseCode = "404", description = "해당 대학을 찾을 수 없음")
  })
  @GetMapping("/university/{universityId}")
  public RsData<List<MajorResponse>> getMajorsByUniversity(@PathVariable Long universityId) {
    List<MajorResponse> majors = majorService.getMajorsByUniversity(universityId);
    return new RsData<>("200", "대학별 전공 목록 조회에 성공했습니다.", majors);
  }


}
