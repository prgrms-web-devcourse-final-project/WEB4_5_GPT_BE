package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentCancelRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentService;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수강 신청, 취소, 내 수강목록 조회 등의
 * 수강 신청 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final Rq rq;

    /**
     * 현재 로그인된 학생의 수강목록을 조회합니다.
     *
     * @return 조회된 수강목록에 해당하는 {@link MyEnrollmentResponse} DTO 리스트
     */
    @GetMapping("/me")
    public RsData<List<MyEnrollmentResponse>> getMyEnrollmentList() {

        Member actor = rq.getActor(); // 인증된 사용자(Actor) 정보 획득
        Member student = rq.getRealActor(actor); // 실제 학생(Member) 객체 얻기 (StudentProfile 필요)

        // 서비스에서 수강목록을 조회하여 반환
        List<MyEnrollmentResponse> response = enrollmentService.getMyEnrollmentList(student);

        return new RsData<>("200", "내 수강목록 조회가 완료되었습니다.", response);
    }

    // 수강 신청
    @PostMapping
    public RsData<Empty> enrollment(@RequestBody EnrollmentRequest request) {
        return new RsData<>("200", "수강 신청이 완료되었습니다.");
    }

    // 수강 취소
    @DeleteMapping("/{courseId}")
    public RsData<Empty> enrollmentCancel(@RequestBody EnrollmentCancelRequest request) {
        return new RsData<>("200", "수강 취소가 완료되었습니다.");
    }

}
