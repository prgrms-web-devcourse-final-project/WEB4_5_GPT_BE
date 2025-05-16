package com.WEB4_5_GPT_BE.unihub.domain.timetable.controller;


import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableBulkRegisterRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableCourseAddRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemNormalCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableShareLinkResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableSharedViewResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.service.TimetableItemService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Timetable", description = "시간표 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
@SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
public class TimetableController {

    private final TimetableService timetableService;
    private final TimetableItemService timetableItemService;
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

    @Operation(
            summary = "내 시간표 조회",
            description = "지정한 연도와 학기에 해당하는 내 시간표를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시간표 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 학기의 시간표가 존재하지 않음"),
            @ApiResponse(responseCode = "400", description = "요청 필드가 유효하지 않음 (예: 학기 범위 오류)")
    })
    @GetMapping("/me")
    public RsData<TimetableDetailResponse> getMyTimetable(
            @RequestParam int year,
            @RequestParam int semester
    ) {
        Member member = rq.getActor();
        TimetableDetailResponse response = timetableService.getMyTimetable(member, year, semester);
        return new RsData<>("200", "시간표 조회 성공", response);
    }

    @Operation(
            summary     = "시간표 공유 링크 생성",
            description = """
                    지정한 시간표 ID로 7일짜리 공유 링크를 만듭니다.
                    *헤더* **`X-Client-Base-Url`** 에 프론트 도메인을 꼭 넣어 주세요.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 링크 생성 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 시간표가 아님"),
            @ApiResponse(responseCode = "404", description = "시간표가 존재하지 않음"),
            @ApiResponse(responseCode = "400", description = "헤더 누락 등 잘못된 요청")
    })
    @PostMapping("/share/link")
    public RsData<TimetableShareLinkResponse> createShareLink(
            @RequestBody @Valid TimetableShareLinkRequest request,
            @RequestHeader("X-Client-Base-Url") String clientBaseUrl
    ) {
        Member member = rq.getActor();
        TimetableShareLinkResponse response = timetableService.createShareLink(member, request, clientBaseUrl);
        return new RsData<>("201", "공유 링크가 생성되었습니다.", response);
    }

    @Operation(
            summary = "공유된 시간표 조회",
            description = """
                    공유 링크(shareKey)를 통해 공개된 시간표를 조회합니다.<br>
                            - 비공개인 경우 403 반환<br>
                            - 만료되었거나 존재하지 않는 경우 404 반환
                        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유된 시간표 조회 성공"),
            @ApiResponse(responseCode = "403", description = "비공개로 설정된 시간표"),
            @ApiResponse(responseCode = "404", description = "만료되었거나 존재하지 않는 공유 링크"),
    })
    @GetMapping("/share/{shareKey}")
    public RsData<TimetableSharedViewResponse> getSharedTimetable(@PathVariable String shareKey) {
        Member member = rq.getActor();
        var response = timetableService.getSharedTimetable(shareKey, member);
        return new RsData<>("200", "공유된 시간표 조회 성공", response);
    }

    @Operation(
            summary = "시간표 수정 항목 등록",
            description = "시간표에 일반 항목(사용자 정의 항목)을 추가합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "항목 등록 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 시간표가 아님"),
            @ApiResponse(responseCode = "404", description = "시간표가 존재하지 않음")
    })
    @PostMapping("/normal")
    public RsData<Empty> createNormalItem(@RequestBody TimetableItemNormalCreateRequest request) {
        Member actor = rq.getActor();
        timetableItemService.createNormalItem(actor, request);
        return new RsData<>("201", "시간표 일정이 성공적으로 등록되었습니다.");
    }

    @Operation(
            summary = "시간표에 강의 등록",
            description = "시간표에 강의 항목을 추가합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "강의 항목 등록 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 시간표가 아님"),
            @ApiResponse(responseCode = "404", description = "시간표 또는 강의가 존재하지 않음"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 강의"
            )
    })
    @PostMapping("/course")
    public RsData<Empty> addCourseItem(@RequestBody TimetableCourseAddRequest request) {
        Member actor = rq.getActor();
        timetableItemService.addCourseItem(actor, request);
        return new RsData<>("201", "강의가 시간표에 성공적으로 등록되었습니다.");
    }

    @Operation(
            summary = "내 강의 목록으로 시간표 일괄 등록",
            description = "내 수강신청 정보를 기반으로 시간표에 강의 항목을 일괄 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "일괄 등록 성공"),
            @ApiResponse(responseCode = "404", description = "시간표가 존재하지 않음")
    })
    @PostMapping("/bulk")
    public RsData<Empty> bulkRegisterFromEnrollment(@RequestBody TimetableBulkRegisterRequest request) {
        Member member = rq.getActor();
        timetableItemService.bulkRegisterFromEnrollment(member, request);
        return new RsData<>("201", "시간표에 반영되었습니다.");
    }

    @Operation(
            summary = "시간표 항목 단건 조회",
            description = "시간표 항목을 단건 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "항목 조회 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 시간표가 아님"),
            @ApiResponse(responseCode = "404", description = "항목이 존재하지 않음")
    })
    @GetMapping("/{timetableItemId}")
    public RsData<TimetableItemDetailResponse> getItemDetail(@PathVariable Long timetableItemId) {
        Member actor = rq.getActor();
        TimetableItemDetailResponse response = timetableItemService.getItemDetail(actor, timetableItemId);
        return new RsData<>("200", "시간표 항목 조회 성공", response);
    }

    @Operation(
            summary = "시간표 항목 수정",
            description = "시간표 항목을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "항목 수정 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 시간표가 아님"),
            @ApiResponse(responseCode = "404", description = "항목이 존재하지 않음")
    })
    @PutMapping("/{timetableItemId}")
    public RsData<TimetableItemUpdateRequest> updateItem(@PathVariable Long timetableItemId, @RequestBody TimetableItemUpdateRequest request) {
        Member actor = rq.getActor();
        timetableItemService.updateItem(actor, timetableItemId, request);
        return null;
    }

    @Operation(
            summary = "시간표 항목 삭제",
            description = "시간표 항목을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "항목 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 시간표가 아님"),
            @ApiResponse(responseCode = "404", description = "항목이 존재하지 않음")
    })
    @DeleteMapping("/{timetableItemId}")
    public RsData<Empty> deleteItem(@PathVariable Long timetableItemId) {
        Member actor = rq.getActor();
        timetableItemService.deleteItem(actor, timetableItemId);
        return new RsData<>("200", "시간표가 성공적으로 삭제되었습니다.");
    }
}
