package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentCancelRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentRequest;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.CourseScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    @GetMapping("/me")
    public RsData<List<MyEnrollmentResponse>> getMyEnrollmentList() {

        var response = List.of(
                new MyEnrollmentResponse(1L, 101L, "컴퓨터공학과", "자료구조", "김교수", "OO동 401호",
                        List.of(
                                new CourseScheduleResponse("MON", "09:00:00", "10:30:00"),
                                new CourseScheduleResponse("FRI", "14:00:00", "15:30:00")
                        ),
                        3, 3, 2, 30, 3
                ),
                new MyEnrollmentResponse(2L, 102L, "컴퓨터공학과", "운영체제", "이교수", "OO동 402호",
                        List.of(
                                new CourseScheduleResponse("TUE", "09:00:00", "10:30:00"),
                                new CourseScheduleResponse("THU", "14:00:00", "15:30:00")
                        ),
                        2, 3, 2, 30, 3
                )
        );
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
