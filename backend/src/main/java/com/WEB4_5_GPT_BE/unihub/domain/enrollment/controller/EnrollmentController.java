package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.course.dto.TimetableCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.StudentEnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentQueueService;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentService;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.EnrollmentApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.EnrollmentCancelApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.GetMyEnrollmentListApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse.getMyEnrollmentPeriodApiResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final EnrollmentQueueService enrollmentQueueService;

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
    @GetMapping("/me")
    public RsData<List<MyEnrollmentResponse>> getMyEnrollmentList(@AuthenticationPrincipal SecurityUser user) {
        // 세션 유효성 검증
        validateEnrollmentSession(user);

        Student actor = Student.builder().id(user.getId()).build();
        List<MyEnrollmentResponse> response = enrollmentService.getMyEnrollmentList(actor); // 내 수강목록을 조회하여 반환

        return new RsData<>("200", "내 수강목록 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "내 수강신청 기간 조회",
            description = "로그인된 학생의 수강신청 기간 정보를 조회합니다. header에 Bearer accessToken이 없다면 접근할 수 없습니다.")
    @getMyEnrollmentPeriodApiResponse // api 요청에 대한 성공,예외 response 예시를 정의합니다.
    @GetMapping("/periods/me")
    public RsData<StudentEnrollmentPeriodResponse> getMyEnrollmentPeriod(@AuthenticationPrincipal SecurityUser user) {
        // 세션 유효성 검증
        validateEnrollmentSession(user);

        Student actor = Student.builder().id(user.getId()).build();
        StudentEnrollmentPeriodResponse response = enrollmentService.getMyEnrollmentPeriod(actor); // 내 수강신청 기간 정보 조회

        return new RsData<>("200", "내 수강신청 기간 정보를 조회했습니다.", response);
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
    public RsData<Empty> enrollmentCancel(@PathVariable Long courseId, @AuthenticationPrincipal SecurityUser user) {
        // 세션 유효성 검증
        validateEnrollmentSession(user);

        Student actor = Student.builder().id(user.getId()).build();
        enrollmentService.cancelMyEnrollment(actor, courseId); // 해당 강좌에 대한 수강 취소 요청

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
    public RsData<Empty> enrollment(@RequestBody EnrollmentRequest request, @AuthenticationPrincipal SecurityUser user) {
        // 세션 유효성 검증
        validateEnrollmentSession(user);

        Student actor = Student.builder().id(user.getId()).build();
        enrollmentService.enrollment(actor, request.courseId()); // 해당 강좌에 대한 수강 신청 요청

        return new RsData<>("200", "수강 신청이 완료되었습니다.");
    }

    @Operation(summary = "내 강의 불러오기 (시간표 등록용 간소화된 response)", description = "주어진 조건에 해당하는 내 강의의 목록을 반환합니다. 필수 쿼리 파라미터(year, semester)가 누락된 경우 400 에러가 발생합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "필수 쿼리 파라미터가 누락됨",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))}),
            @ApiResponse(responseCode = "500", description = "조회 실패; 인증된 유저의 데이터 또는 쿼리 파라미터가 잘못됨",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))})
    })
    @GetMapping("/timetables/me")
    public RsData<List<TimetableCourseResponse>> getMyEnrollmentsForTimetable(
            @Parameter(required = true, description = "조회할 연도", example = "2025")
            @RequestParam(value = "year", required = false) Integer year,
            @Parameter(required = true, description = "조회할 학기", example = "1")
            @RequestParam(value = "semester", required = false) Integer semester,
            @AuthenticationPrincipal SecurityUser actor
    ) {
        // 필수 파라미터 검증
        if (year == null) {
            throw new RequiredParameterMissingException("year");
        }
        if (semester == null) {
            throw new RequiredParameterMissingException("semester");
        }

        List<TimetableCourseResponse> timetableCourseResponse =
                enrollmentService.getMyEnrollmentsForTimetable(actor, year, semester);

        return new RsData<>("200", "시간표 등록용 강의 목록 조회 완료", timetableCourseResponse);
    }

    /**
     * 사용자의 수강신청 세션 유효성 검증
     * 세션이 없으면 예외 발생
     */
    private void validateEnrollmentSession(SecurityUser user) {
        String memberId = String.valueOf(user.getId());
        QueueStatusDto queueStatus = enrollmentQueueService.getQueueStatus(memberId);

        if (!queueStatus.isAllowed()) {
            throw new NoSessionException();
        }
    }
}
