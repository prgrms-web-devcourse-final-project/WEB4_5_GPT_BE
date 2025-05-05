package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentService;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.exception.enrollment.EnrollmentNotFoundException;
import com.WEB4_5_GPT_BE.unihub.global.exception.enrollment.EnrollmentPeriodClosedException;
import com.WEB4_5_GPT_BE.unihub.global.exception.enrollment.EnrollmentPeriodNotFoundException;
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
     * 내 수강신청 목록을 조회합니다.
     * 로그인된 사용자만 접근 가능합니다.
     *
     * @return 조회된 수강목록에 해당하는 {@link MyEnrollmentResponse} DTO 리스트
     */
    @GetMapping("/me")
    public RsData<List<MyEnrollmentResponse>> getMyEnrollmentList() {

        Member actor = rq.getActor(); // 인증된 사용자(Actor) 정보 획득
        Member student = rq.getRealActor(actor); // 실제 학생(Member) 객체 얻기 (StudentProfile 필요)

        // 내 수강목록을 조회하여 반환
        List<MyEnrollmentResponse> response = enrollmentService.getMyEnrollmentList(student);

        return new RsData<>("200", "내 수강목록 조회가 완료되었습니다.", response);
    }

    /**
     * 수강 취소 요청을 처리합니다.
     * 로그인된 사용자만 접근 가능합니다.
     *
     * @param courseId 취소할 강좌의 ID
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     * @throws EnrollmentNotFoundException       수강신청 내역이 없는 경우
     */
    @DeleteMapping("/{courseId}")
    public RsData<Empty> enrollmentCancel(@PathVariable Long courseId) {

        Member actor = rq.getActor(); // 인증된 사용자(Actor) 정보 획득
        Member student = rq.getRealActor(actor); // 실제 학생(Member) 객체 얻기 (StudentProfile 필요)

        // 해당 강좌에 대한 수강 취소 요청
        enrollmentService.cancelMyEnrollment(student, courseId);

        return new RsData<>("200", "수강 취소가 완료되었습니다.");
    }

    // 수강 신청
    @PostMapping
    public RsData<Empty> enrollment(@RequestBody EnrollmentRequest request) {
        return new RsData<>("200", "수강 신청이 완료되었습니다.");
    }

}
