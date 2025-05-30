package com.WEB4_5_GPT_BE.unihub.domain.course.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.CourseListReturnMode;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithFullScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithOutUrlRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.service.CourseService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 강의 도메인 컨트롤러 레이어.
 */
@Tag(name = "Course", description = "강의 도메인 API 엔드포인트")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
public class CourseController {

    private final CourseService courseService;

    /**
     * 주어진 ID에 해당하는 강의를 조회한다.
     *
     * @param courseId 조회하고자 하는 강의의 ID
     * @return 조회된 강의에 해당하는 {@link CourseWithFullScheduleResponse} DTO
     */
    @Operation(summary = "강의 단건 조회", description = "단건의 강의에 대한 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "조회 실패; 주어진 ID에 해당하는 강의가 존재하지 않음",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))})
    })
    @GetMapping("/{courseId}")
    public RsData<CourseWithFullScheduleResponse> getCourse(@PathVariable Long courseId) {
        CourseWithFullScheduleResponse res = courseService.getCourseWithFullScheduleById(courseId);
        return new RsData<>(String.valueOf(HttpStatus.OK.value()), "성공적으로 조회되었습니다.", res);
    }

    /**
     * 주어진 정보를 바탕으로 새 강의를 생성합니다.
     * - 강의계획서 파일(`file` 파트)을 전송하면 AWS S3에 업로드 후
     * 발급된 URL을 `coursePlanAttachment` 필드에 저장합니다.
     * - 강의실/교수 스케줄 충돌 여부 및 전공·교수 유효성을 검증한 뒤 저장합니다.
     *
     * @param courseWithOutUrlRequest  생성할 강의의 상세 데이터 (JSON, `data` 파트)
     * @param coursePlanFile 강의계획서 파일 (multipart `file` 파트)
     * @return 생성된 강의 정보를 담은 {@link CourseWithFullScheduleResponse}
     */
    @Operation(summary = "새 강의 생성",
            description = "새로운 강의를 생성합니다. 강의계획서 파일을 함께 전송하면 S3에 업로드하여 URL을 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "생성 실패; 소속 대학, 전공, 또는 담당 교수 정보가 잘못됨",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "409", description = "생성 실패; 강의 스케줄이 기존 강의실 또는 교수 스케줄과 겹침",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<CourseWithFullScheduleResponse> createCourse(
            @Parameter(description = "생성할 강의 정보를 담은 JSON", content = @Content(mediaType = "application/json"))
            @RequestPart("data") CourseWithOutUrlRequest courseWithOutUrlRequest,

            @Parameter(description = "강의계획서 파일", content = @Content(mediaType = "application/octet-stream"))
            @RequestPart(name = "file", required = false) MultipartFile coursePlanFile,

            @AuthenticationPrincipal SecurityUser principal
    ) {
        CourseWithFullScheduleResponse res = courseService.createCourse(courseWithOutUrlRequest, coursePlanFile, principal);
        return new RsData<>(String.valueOf(HttpStatus.CREATED.value()), "성공적으로 생성되었습니다.", res);
    }

    /**
     * 주어진 강의 정보를 바탕으로 기존 강의를 수정합니다.
     * - 강의계획서 파일(`file`)이 함께 전송되면, AWS S3에 업로드 후
     * 새로운 URL로 `coursePlanAttachment`를 교체합니다.
     * - 스케줄, 전공/교수 등 유효성 검사 후 충돌이 없으면 덮어씌우기 방식으로 업데이트합니다.
     *
     * @param courseId       수정 대상 강의의 ID
     * @param courseWithOutUrlRequest  수정할 강의의 상세 데이터 (JSON, `data` 파트)
     * @param coursePlanFile 새로운 강의계획서 파일 (multipart `file` 파트)
     * @return 수정된 강의 정보를 담은 {@link CourseWithFullScheduleResponse}
     */
    @Operation(
            summary = "강의 수정",
            description = "기존 강의를 수정합니다. 강의계획서 파일을 함께 전송하면 S3에 업로드하여 URL을 교체합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정 실패; 소속 대학, 전공, 또는 담당 교수 정보가 잘못됨",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "수정 실패; 주어진 ID에 해당하는 강의가 존재하지 않음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "409", description = "수정 실패; 새 강의 스케줄이 기존 강의실 또는 교수 스케줄과 겹침",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class)))
    })
    @PutMapping(path = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<CourseWithFullScheduleResponse> updateCourse(
            @PathVariable Long courseId,

            @Parameter(description = "수정할 강의 정보를 담은 JSON", content = @Content(mediaType = "application/json"))
            @RequestPart("data") CourseWithOutUrlRequest courseWithOutUrlRequest,

            @Parameter(description = "새로운 강의계획서 파일", content = @Content(mediaType = "application/octet-stream"))
            @RequestPart(name = "file", required = false) MultipartFile coursePlanFile,

            @AuthenticationPrincipal SecurityUser principal
    ) {
        CourseWithFullScheduleResponse res = courseService.updateCourse(courseId, courseWithOutUrlRequest, coursePlanFile, principal);
        return new RsData<>(String.valueOf(HttpStatus.OK.value()), "성공적으로 수정되었습니다.", res);
    }

    /**
     * 주어진 ID에 해당하는 강의를 삭제합니다.
     * 강의에 강의계획서 파일이 등록되어 있으면, 해당 파일을 AWS S3에서 먼저 삭제한 후
     * DB의 강의 데이터를 삭제합니다.
     *
     * @param courseId 삭제하고자 하는 강의의 ID
     */
    @Operation(summary = "강의 삭제", description = "단건의 강의를 삭제합니다. 강의계획서 파일이 있으면 S3에서도 함께 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "삭제 실패; 수강신청이 한 개라도 되어있는 강의는 삭제할 수 없습니다.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "삭제 실패; 주어진 ID에 해당하는 강의가 존재하지 않습니다.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class)))
    })
    @DeleteMapping("/{courseId}")
    public RsData<Empty> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return new RsData<>(String.valueOf(HttpStatus.OK.value()), "성공적으로 삭제되었습니다.");
    }

    /**
     * 주어진 필터링/페이지네이션 정보와 인증 유저 정보를 바탕으로 강의 목록을 조회한다.
     *
     * @param mode      조회 모드(반환할 DTO의 종류)
     * @param title     강의 제목 필터링 문자열
     * @param profName  교수 이름 필터링 문자열
     * @param principal 인증된 유저 정보
     * @param pageable  페이지네이션 정보
     * @return {@code mode}에서 명시된 타입의 DTO가 담긴 {@link Page} 오브젝트
     */
    @Operation(summary = "강의 목록 조회", description = "주어진 조건에 해당하는 강의의 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "조회 실패; 인증된 유저의 데이터 또는 쿼리 파라미터가 잘못됨",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = RsData.class))})
    })
    @GetMapping
    public RsData<Page<?>> getAllCourses(
            @Parameter(required = true, description = "반환할 목록 형식: FULL, ENROLL, CATALOG, TIMETABLE 중 하나", example = "FULL")
            @RequestParam("mode") CourseListReturnMode mode,

            @Parameter(description = "강의 제목 키워드 (검색용)", example = "자료구조")
            @RequestParam(name = "title", defaultValue = "") String title,

            @Parameter(description = "교수 이름 키워드 (검색용)", example = "홍길동")
            @RequestParam(name = "profName", defaultValue = "") String profName,

            @Parameter(description = "전공 ID (숫자)", example = "3")
            @RequestParam(required = false) Long majorId,

            @Parameter(description = "학년 (1~4 중 하나)", example = "2")
            @RequestParam(required = false) Integer grade,

            @Parameter(description = "학기 (1 또는 2)", example = "1")
            @RequestParam(required = false) Integer semester,


            @AuthenticationPrincipal SecurityUser principal,
            @Parameter(hidden = true)
            @PageableDefault @ParameterObject Pageable pageable) {
        return switch (mode) {
            case FULL -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeFull(title, profName, majorId, grade, semester, principal, pageable));
            case ENROLL -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeEnroll(title, profName, majorId, grade, semester, principal, pageable));
            case CATALOG -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeCatalog(title, profName, majorId, grade, semester, principal, pageable));
            case TIMETABLE -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeTimetable(title, profName, majorId, grade, semester, principal, pageable));
        };
    }
}
