package com.WEB4_5_GPT_BE.unihub.domain.course.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.CourseListReturnMode;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithFullScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.service.CourseService;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 강의 도메인 컨트롤러 레이어.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 주어진 ID에 해당하는 강의를 조회한다.
     * @param courseId 조회하고자 하는 강의의 ID
     * @return 조회된 강의에 해당하는 {@link CourseWithFullScheduleResponse} DTO
     */
    @GetMapping("/{courseId}")
    public RsData<CourseWithFullScheduleResponse> getCourse(@PathVariable Long courseId) {
        CourseWithFullScheduleResponse res = courseService.getCourseWithFullScheduleById(courseId);
        return new RsData<>(String.valueOf(HttpStatus.OK.value()), "성공적으로 조회되었습니다.", res);
    }

    /**
     * 주어진 정보를 바탕으로 스케줄을 검증한 뒤 새 강의를 생성한다.
     * @param courseRequest 생성하고자 하는 강의의 정보
     * @return 생성된 강의에 해당하는 {@link CourseWithFullScheduleResponse} DTO
     */
    @PostMapping
    public RsData<CourseWithFullScheduleResponse> createCourse(@RequestBody CourseRequest courseRequest) {
        CourseWithFullScheduleResponse res = courseService.createCourse(courseRequest);
        return new RsData<>(String.valueOf(HttpStatus.CREATED.value()), "성공적으로 생성되었습니다.", res);
    }

    /**
     * 주어진 강의 정보의 스케줄을 검증한 뒤 주어진 ID에 해당하는 강의위에 덮어씌운다.
     * @param courseId 덮어쓰고자 하는 강의의 ID
     * @param courseRequest 덮어씌우고자 하는 강의 정보
     * @return 갱신된 강의에 해당하는 {@link CourseWithFullScheduleResponse} DTO
     */
    @PutMapping("/{courseId}")
    public RsData<CourseWithFullScheduleResponse> updateCourse(@PathVariable Long courseId, @RequestBody CourseRequest courseRequest) {
        CourseWithFullScheduleResponse res = courseService.updateCourse(courseId, courseRequest);
        return new RsData<>(String.valueOf(HttpStatus.OK.value()), "성공적으로 수정되었습니다.", res);
    }

    /**
     * 주어진 ID에 해당하는 강의를 삭제한다.
     * @param courseId 삭제하고자 하는 강의의 ID
     * @return 해당 없음
     */
    @DeleteMapping("/{courseId}")
    public RsData<Empty> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return new RsData<>(String.valueOf(HttpStatus.OK.value()), "성공적으로 삭제되었습니다.");
    }

    /**
     * 주어진 필터링/페이지네이션 정보와 인증 유저 정보를 바탕으로 강의 목록을 조회한다.
     * @param mode 조회 모드(반환할 DTO의 종류)
     * @param title 강의 제목 필터링 문자열
     * @param profName 교수 이름 필터링 문자열
     * @param principal 인증된 유저 정보
     * @param pageable 페이지네이션 정보
     * @return {@code mode}에서 명시된 타입의 DTO가 담긴 {@link Page} 오브젝트
     */
    @GetMapping
    public RsData<Page<?>> getAllCourses(
        @RequestParam("mode") CourseListReturnMode mode,
        @RequestParam(name = "title", defaultValue = "") String title,
        @RequestParam(name = "profName", defaultValue = "") String profName,
        @AuthenticationPrincipal SecurityUser principal,
        @PageableDefault Pageable pageable) {
        // TODO: 인증이 안되어있는 상태에서 요청이 들어오면 인증 정보에서 소속 대학ID를 꺼내오는 과정에서 NPE가 발생한다.
//        if (principal == null) {
//            return new RsData<>(String.valueOf(HttpStatus.UNAUTHORIZED.value()), "인증이 필요합니다.");
//        }
        return switch (mode) {
            case FULL -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeFull(title, profName, principal, pageable));
            case ENROLL -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeEnroll(title, profName, principal, pageable));
            case CATALOG -> new RsData<>(String.valueOf(HttpStatus.OK.value()),
                    "조회에 성공했습니다.",
                    courseService.findAllCoursesModeCatalog(title, profName, principal, pageable));
        };
    }
}
