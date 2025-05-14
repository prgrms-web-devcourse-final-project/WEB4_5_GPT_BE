package com.WEB4_5_GPT_BE.unihub.domain.timetable.controller;


import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.service.TimetableService;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Timetable", description = "시간표 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
@SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
public class TimetableController {

    private final TimetableService timetableService;
    private final Rq rq;

    @Operation(
            summary = "시간표 생성",
            description = "사용자의 시간표를 생성합니다. (연도 + 학기 기준으로 중복 생성 불가)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "시간표 생성 성공"),
            @ApiResponse(responseCode = "409", description = "이미 해당 학기의 시간표가 존재합니다"),
            @ApiResponse(responseCode = "400", description = "요청 필드가 유효하지 않음 (예: 연도 또는 학기 누락)")
    })
    @PostMapping
    public RsData<Empty> createTimetable(@RequestBody @Valid TimetableCreateRequest request) {
        Member member = rq.getActor();
        timetableService.createTimetable(member, request);
        return new RsData<>("201", "시간표가 성공적으로 생성되었습니다.");
    }
}
