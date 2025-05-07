package com.WEB4_5_GPT_BE.unihub.domain.admin.controller;

import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.EnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.ProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.StudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.service.AdminService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final MajorService majorService;
    private final UniversityService universityService;

    /**
     * 학생 회원 목록 조회
     */
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

    /**
     * 교직원 등록 신청 조회
     */
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

    /**
     * 교직원 등록 상태 변경
     */
    @PatchMapping("/professors/{memberId}")
    public RsData<Void> changeProfessorStatus(
            @PathVariable Long memberId, @RequestBody ProfessorApprovalRequest request) {
        adminService.changeProfessorStatus(memberId, request);
        return new RsData<>("200", "교직원 상태가 변경되었습니다.");
    }

    /**
     * 수강신청 기간 조회
     */
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

    /**
     * 수강신청기간 관리 등록
     */
    @PostMapping("/enrollment-periods")
    public RsData<EnrollmentPeriodResponse> createEnrollmentPeriod(
            @RequestBody EnrollmentPeriodRequest request) {
        EnrollmentPeriodResponse period = adminService.createEnrollmentPeriod(request);
        return new RsData<>("201", "수강신청 기간이 등록되었습니다.", period);
    }

    /**
     * 수강신청기간 관리 수정
     */
    @PutMapping("/enrollment-periods/{periodId}")
    public RsData<EnrollmentPeriodResponse> updateEnrollmentPeriod(
            @PathVariable Long periodId, @RequestBody EnrollmentPeriodRequest request) {
        EnrollmentPeriodResponse period = adminService.updateEnrollmentPeriod(periodId, request);
        return new RsData<>("200", "수강신청 기간이 수정되었습니다.", period);
    }

    /**
     * 수강신청기간 관리 삭제
     */
    @DeleteMapping("/enrollment-periods/{periodId}")
    public RsData<Void> deleteEnrollmentPeriod(@PathVariable Long periodId) {
        adminService.deleteEnrollmentPeriod(periodId);
        return new RsData<>("200", "수강신청 기간이 삭제되었습니다.");
    }

  /** 관리자 초대 */
  @PostMapping("/invite")
  public RsData<Void> inviteAdmin(@RequestBody AdminInviteRequest request) {
    adminService.inviteAdmin(request);
    return new RsData<>("200", "관리자 초대가 완료되었습니다.");
  }
}
