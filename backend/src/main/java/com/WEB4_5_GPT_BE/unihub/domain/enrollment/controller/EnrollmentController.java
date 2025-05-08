package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentService;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.EnrollmentApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.EnrollmentCancelApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.GetMyEnrollmentListApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수강 신청, 취소, 내 수강목록 조회 등의
 * 수강 신청 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@Tag(name = "Enrollment", description = "수강신청 관련 API (수강신청, 취소, 내 수강목록 조회)")
@SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증") // 해당 controller의 모든 Api에 accessToken 로그인이 필요하여 전역 적용함
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/enrollments", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final Rq rq;

    /**
     * 내 수강신청 목록을 조회합니다.
     * 로그인된 사용자만 접근 가능합니다.
     *
     * @return 조회된 수강목록에 해당하는 {@link MyEnrollmentResponse} DTO 리스트
     */
    @Operation(
            summary = "내 수강신청 목록 조회",
            description = "로그인된 학생 본인의 수강신청 목록을 조회합니다. header에 Bearer accessToken이 없다면 접근할 수 없습니다."
    )
    @GetMyEnrollmentListApiResponse // api 요청에 대한 성공,예외 response 예시를 정의합니다.
    @GetMapping(value = "/me")
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
    @Operation(
            summary = "수강 취소",
            description = "로그인된 학생이 특정 강좌 수강을 취소합니다. header에 Bearer accessToken이 없다면 접근할 수 없습니다."
    )
    @EnrollmentCancelApiResponse // api 요청에 대한 성공,예외 response 예시를 정의합니다.
    @DeleteMapping("/{courseId}")
    public RsData<Empty> enrollmentCancel(@PathVariable Long courseId) {

        Member actor = rq.getActor(); // 인증된 사용자(Actor) 정보 획득
        Member student = rq.getRealActor(actor); // 실제 학생(Member) 객체 얻기 (StudentProfile 필요)

        // 해당 강좌에 대한 수강 취소 요청
        enrollmentService.cancelMyEnrollment(student, courseId);

        return new RsData<>("200", "수강 취소가 완료되었습니다.");
    }

    /**
     * 수강 신청 요청을 처리합니다.
     * 로그인된 사용자만 접근 가능합니다.
     *
     * @param request 수강 신청할 강좌 정보
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     * @throws CourseNotFoundException           강좌 정보가 없는 경우
     * @throws CourseCapacityExceededException   정원 초과 시
     * @throws DuplicateEnrollmentException      동일 강좌 중복 신청 시
     * @throws CreditLimitExceededException      최대 학점 초과 시
     * @throws ScheduleConflictException         기존 신청한 강좌와 시간표가 겹치는 경우
     */
    @Operation(
            summary = "수강 신청",
            description = "로그인된 학생이 특정 강좌에 수강 신청을 합니다. header에 Bearer accessToken이 없다면 접근할 수 없습니다."
    )
    @EnrollmentApiResponse // api 요청에 대한 성공,예외 response 예시를 정의합니다.
    @PostMapping
    public RsData<Empty> enrollment(@RequestBody EnrollmentRequest request) {

        Member actor = rq.getActor(); // 인증된 사용자(Actor) 정보 획득
        Member student = rq.getRealActor(actor); // 실제 학생(Member) 객체 얻기 (StudentProfile 필요)

        // 해당 강좌에 대한 수강 신청 요청
        enrollmentService.enrollment(student, request.courseId());

        return new RsData<>("200", "수강 신청이 완료되었습니다.");
    }

}
