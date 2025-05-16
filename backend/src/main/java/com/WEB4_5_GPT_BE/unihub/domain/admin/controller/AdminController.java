package com.WEB4_5_GPT_BE.unihub.domain.admin.controller;

import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.EnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.ProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.StudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.service.AdminService;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.MajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.UniversityRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "관리자 관련 API (학생/교수 관리, 수강신청 기간 관리, 관리자 초대 등)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UniversityService universityService;
    private final MajorService majorService;

    @Operation(summary = "학생 회원 목록 조회", description = "다양한 조건으로 학생 회원 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음")
    })
    @GetMapping("/students")
    public RsData<Page<StudentResponse>> getStudents(
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) Integer semester,
            @PageableDefault Pageable pageable) {

        StudentSearchRequest searchRequest =
                new StudentSearchRequest(universityId, majorId, grade, semester);
        Page<StudentResponse> students = adminService.getStudents(searchRequest, pageable);
        return new RsData<>("200", "회원 목록 조회에 성공했습니다.", students);
    }

    @Operation(summary = "교직원 등록 신청 조회", description = "다양한 조건으로 교직원 등록 신청 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "교직원 등록 신청 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음")
    })
    @GetMapping("/professors")
    public RsData<Page<ProfessorResponse>> getProfessors(
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) String professorName,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String status,
            @PageableDefault Pageable pageable) {

        com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus approvalStatus = null;
        if (status != null) {
            approvalStatus = com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus.valueOf(status);
        }

        ProfessorSearchRequest searchRequest =
                new ProfessorSearchRequest(universityId, professorName, majorId, approvalStatus);
        Page<ProfessorResponse> professors = adminService.getProfessors(searchRequest, pageable);
        return new RsData<>("200", "교직원 등록 신청 조회가 완료되었습니다.", professors);
    }

    @Operation(summary = "교직원 등록 상태 변경", description = "교직원의 등록 상태를 승인 또는 거절로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
    })
    @PatchMapping("/professors/{memberId}")
    public RsData<Void> changeProfessorStatus(
            @PathVariable Long memberId, @RequestBody ProfessorApprovalRequest request) {
        adminService.changeProfessorStatus(memberId, request);
        return new RsData<>("200", "교직원 상태가 변경되었습니다.");
    }

    @Operation(summary = "수강신청 기간 조회", description = "등록된 수강신청 기간을 검색 조건에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음")
    })
    @GetMapping("/enrollment-periods")
    public RsData<Page<EnrollmentPeriodResponse>> getEnrollmentPeriods(
            @RequestParam(required = false) String universityName,
            @RequestParam(required = false) String startDateFrom,
            @RequestParam(required = false) String startDateTo,
            @RequestParam(required = false) String endDateFrom,
            @RequestParam(required = false) String endDateTo,
            @PageableDefault Pageable pageable) {

        EnrollmentPeriodSearchRequest searchRequest =
                new EnrollmentPeriodSearchRequest(
                        universityName, startDateFrom, startDateTo, endDateFrom, endDateTo);
        Page<EnrollmentPeriodResponse> periods =
                adminService.getEnrollmentPeriods(searchRequest, pageable);
        return new RsData<>("200", "수강신청 기간 조회가 완료되었습니다.", periods);
    }

    @Operation(summary = "수강신청 기간 등록", description = "새로운 수강신청 기간을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (기간 중복, 유효하지 않은 날짜 형식)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "대학교를 찾을 수 없음")
    })
    @PostMapping("/enrollment-periods")
    public RsData<EnrollmentPeriodResponse> createEnrollmentPeriod(
            @RequestBody EnrollmentPeriodRequest request) {
        EnrollmentPeriodResponse period = adminService.createEnrollmentPeriod(request);
        return new RsData<>("201", "수강신청 기간이 등록되었습니다.", period);
    }

    @Operation(summary = "수강신청 기간 수정", description = "등록된 수강신청 기간 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (기간 중복, 유효하지 않은 날짜 형식)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "수강신청 기간 정보를 찾을 수 없음")
    })
    @PutMapping("/enrollment-periods/{periodId}")
    public RsData<EnrollmentPeriodResponse> updateEnrollmentPeriod(
            @PathVariable Long periodId, @RequestBody EnrollmentPeriodRequest request) {
        EnrollmentPeriodResponse period = adminService.updateEnrollmentPeriod(periodId, request);
        return new RsData<>("200", "수강신청 기간이 수정되었습니다.", period);
    }

    @Operation(summary = "수강신청 기간 삭제", description = "등록된 수강신청 기간을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "수강신청 기간 정보를 찾을 수 없음")
    })
    @DeleteMapping("/enrollment-periods/{periodId}")
    public RsData<Void> deleteEnrollmentPeriod(@PathVariable Long periodId) {
        adminService.deleteEnrollmentPeriod(periodId);
        return new RsData<>("200", "수강신청 기간이 삭제되었습니다.");
    }

  @Operation(summary = "관리자 초대", description = "새로운 관리자를 초대합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "초대 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식 오류)"),
          @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
          @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
          @ApiResponse(responseCode = "409", description = "이미 등록된 이메일")
  })
  @PostMapping("/invite")
  public RsData<Void> inviteAdmin(@RequestBody AdminInviteRequest request) {
      adminService.inviteAdmin(request);
      return new RsData<>("200", "관리자 초대가 완료되었습니다.");
  }

    // 대학 관리 API
    @Operation(summary = "대학 생성", description = "새로운 대학을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "대학 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 대학 이름 또는 이메일 도메인")
    })
    @PostMapping("/universities")
    public RsData<UniversityResponse> createUniversity(@RequestBody UniversityRequest request) {
        UniversityResponse university = universityService.createUniversity(request);
        return new RsData<>("201", "대학이 등록되었습니다.", university);
    }

    @Operation(summary = "대학 수정", description = "대학 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대학 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "대학을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 대학 이름 또는 이메일 도메인")
    })
    @PutMapping("/universities/{universityId}")
    public RsData<UniversityResponse> updateUniversity(
            @PathVariable Long universityId, @RequestBody UniversityRequest request) {
        UniversityResponse university = universityService.updateUniversity(universityId, request);
        return new RsData<>("200", "대학 정보가 수정되었습니다.", university);
    }

    @Operation(summary = "대학 삭제", description = "대학을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대학 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "대학을 찾을 수 없음")
    })
    @DeleteMapping("/universities/{universityId}")
    public RsData<Void> deleteUniversity(@PathVariable Long universityId) {
        universityService.deleteUniversity(universityId);
        return new RsData<>("200", "대학이 삭제되었습니다.");
    }

    // 전공 관리 API
    @Operation(summary = "전공 생성", description = "새로운 전공을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "전공 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 전공 이름")
    })
    @PostMapping("/majors")
    public RsData<MajorResponse> createMajor(@RequestBody MajorRequest request) {
        MajorResponse major = majorService.createMajor(request);
        return new RsData<>("201", "전공이 등록되었습니다.", major);
    }

    @Operation(summary = "전공 수정", description = "전공 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전공 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "전공을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 전공 이름")
    })
    @PutMapping("/majors/{majorId}")
    public RsData<MajorResponse> updateMajor(
            @PathVariable Long majorId, @RequestBody MajorRequest request) {
        MajorResponse major = majorService.updateMajor(majorId, request);
        return new RsData<>("200", "전공 정보가 수정되었습니다.", major);
    }

    @Operation(summary = "전공 삭제", description = "전공을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전공 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "전공을 찾을 수 없음")
    })
    @DeleteMapping("/majors/{majorId}")
    public RsData<Void> deleteMajor(@PathVariable Long majorId) {
        majorService.deleteMajor(majorId);
        return new RsData<>("200", "전공이 삭제되었습니다.");
    }
}
