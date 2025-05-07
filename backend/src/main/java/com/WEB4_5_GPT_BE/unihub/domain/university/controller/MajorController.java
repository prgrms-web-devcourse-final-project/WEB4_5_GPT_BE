package com.WEB4_5_GPT_BE.unihub.domain.university.controller;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
public class MajorController {

  private final MajorService majorService;

  /** 전공 목록 조회 (전체) */
  @GetMapping
  public RsData<List<MajorResponse>> getAllMajors() {
    List<MajorResponse> majors = majorService.getAllMajors();
    return new RsData<>("200", "전공 목록 조회에 성공했습니다.", majors);
  }

  /** 대학별 전공 목록 조회 (전체) */
  @GetMapping("/university/{universityId}")
  public RsData<List<MajorResponse>> getMajorsByUniversity(@PathVariable Long universityId) {
    List<MajorResponse> majors = majorService.getMajorsByUniversity(universityId);
    return new RsData<>("200", "대학별 전공 목록 조회에 성공했습니다.", majors);
  }


}
