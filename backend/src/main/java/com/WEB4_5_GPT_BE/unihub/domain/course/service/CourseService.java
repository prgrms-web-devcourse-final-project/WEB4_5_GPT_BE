package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.dto.*;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.FileUploadException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseScheduleRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

/**
 * 강의 도메인 서비스 레이어.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;

    private final CourseScheduleRepository courseScheduleRepository;

    private final MajorRepository majorRepository;

    private final UniversityRepository universityRepository;

    private final StudentRepository studentRepository;

    private final ProfessorRepository professorRepository;

    private final S3Service s3Service;

    /**
     * 주어진 ID에 해당하는 강의 정보를 반환한다.
     *
     * @param courseId 조회하고자 하는 강의의 ID
     * @return 조회된 강의의 {@link CourseWithFullScheduleResponse} DTO
     */
    @Transactional(readOnly = true)
    public CourseWithFullScheduleResponse getCourseWithFullScheduleById(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.NOT_FOUND.value()), "해당 강의가 존재하지 않습니다.")
        );
        return CourseWithFullScheduleResponse.from(course);
    }

    /**
     * 주어진 강의 정보를 바탕으로 스케줄을 검증한 뒤 새 강의를 생성, 영속한다.
     *
     * @param courseRequest 강의 정보
     * @return 생성된 강의의 {@link CourseWithFullScheduleResponse} DTO
     */
    public CourseWithFullScheduleResponse createCourse(CourseRequest courseRequest) {
        // TODO: DRY?
        University u = universityRepository.findByName(courseRequest.university()).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "존재하지 않는 대학교입니다.")
        );
        Major m = majorRepository.findByUniversityIdAndName(u.getId(), courseRequest.major()).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "존재하지 않는 전공입니다.")
        );
        if (doesLocationScheduleConflict(courseRequest.schedule(), u.getId(), courseRequest.location())) {
            throw new UnihubException(String.valueOf(HttpStatus.CONFLICT.value()), "강의 장소가 이미 사용 중입니다.");
        }
        Professor p = courseRequest.employeeId() == null ?
                null :
                professorRepository.findByUniversityIdAndEmployeeId(u.getId(), courseRequest.employeeId()).orElseThrow(
                        () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "존재하지 않는 교수입니다.")
                );
        if (p != null && doesProfScheduleConflict(courseRequest.schedule(), p.getEmployeeId())) {
            throw new UnihubException(String.valueOf(HttpStatus.CONFLICT.value()), "강사/교수가 이미 수업 중입니다.");
        }
        Course res = courseRequest.toEntity(m, 0, p);
        return CourseWithFullScheduleResponse.from(courseRepository.save(res));
    }

    public CourseWithFullScheduleResponse createCourse(CourseRequest req, MultipartFile file) {

        String url = null;

        try {
            if (file != null && !file.isEmpty()) {
                url = s3Service.upload(file);
                req = req.withCoursePlanAttachment(url);
            }
            return createCourse(req); // 기존 createCourse(CourseRequest) 호출
        } catch (IOException e) {
            throw new FileUploadException();
        } catch (UnihubException ex) {
            if (url != null) {
                s3Service.deleteByUrl(url); // 실패 시 업로드 롤백
            }
            throw ex;
        }
    }

    /**
     * 주어진 강의 정보를 주어진 ID에 해당하는 강의 위에 덮어쓴다.
     *
     * @param courseId      덮어쓰고자 하는 강의의 ID
     * @param courseRequest 덮어쓸 정보
     * @return 수정된 강의의 {@link CourseWithFullScheduleResponse} DTO
     */
    public CourseWithFullScheduleResponse updateCourse(Long courseId, CourseRequest courseRequest) {
        Course orig = courseRepository.findById(courseId).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.NOT_FOUND.value()), "해당 강의가 존재하지 않습니다."));
        University u = universityRepository.findByName(courseRequest.university()).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "존재하지 않는 대학교입니다.")
        );
        Major m = majorRepository.findByUniversityIdAndName(u.getId(), courseRequest.major()).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "존재하지 않는 전공입니다.")
        );
        if (doesLocationScheduleConflictExcludingCourse(courseRequest.schedule(), u.getId(), courseRequest.location(), courseId)) {
            throw new UnihubException(String.valueOf(HttpStatus.CONFLICT.value()), "강의 장소가 이미 사용 중입니다.");
        }
        Professor p = courseRequest.employeeId() == null ?
                null :
                professorRepository.findByUniversityIdAndEmployeeId(u.getId(), courseRequest.employeeId()).orElseThrow(
                        () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "존재하지 않는 교수입니다.")
                );
        if (p != null && doesProfScheduleConflictExcludingCourse(courseRequest.schedule(), p.getEmployeeId(), courseId)) {
            throw new UnihubException(String.valueOf(HttpStatus.CONFLICT.value()), "강사/교수가 이미 수업 중입니다.");
        }
        Course res = courseRequest.toEntity(m, orig.getEnrolled(), p);
        res.setId(orig.getId());
        return CourseWithFullScheduleResponse.from(courseRepository.save(res));
    }

    public CourseWithFullScheduleResponse updateCourse(Long courseId, CourseRequest courseRequest, MultipartFile file) {
        String url = null;
        try {
            if (file != null && !file.isEmpty()) {
                url = s3Service.upload(file);
                courseRequest = courseRequest.withCoursePlanAttachment(url);
            }
            return updateCourse(courseId, courseRequest); // 기존 updateCourse(courseId, courseRequest) 호출
        } catch (IOException e) {
            throw new FileUploadException();
        } catch (UnihubException ex) {
            if (url != null) {
                s3Service.deleteByUrl(url); // 실패 시 업로드 롤백
            }
            throw ex;
        }
    }

    /**
     * 주어진 ID에 해당하는 강의를 삭제한다.
     *
     * @param courseId 삭제하고자 하는 강의의 ID
     */
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.NOT_FOUND.value()), "해당 강의가 존재하지 않습니다.")
        );

        // s3에 업로드된 강의계획서 파일 삭제
        // 실패 시에도 강의 삭제는 정상 진행되도록 구현하고 log에 남김
        String attachment = course.getCoursePlanAttachment();
        if (attachment != null) {
            try {
                s3Service.deleteByUrl(attachment);
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (but ignoring): {}", attachment, e);
            }
        }
        courseRepository.delete(course);
    }

    /**
     * 주어진 필터링/페이지네이션 정보를 바탕으로, 인증되어 있는 유저의 소속 대학에서 제공되고 있는 강의 목록을 반환한다.
     *
     * @param title     강의 이름 필터링 문자열
     * @param profName  교수 이름 필터링 문자열
     * @param principal 인증되어있는 유저
     * @param pageable  페이지네이션 정보
     * @return 조회된 강의에 해당하는 {@link CourseWithFullScheduleResponse} DTO가 담긴 {@link Page} 오브젝트
     */
    // TODO: 응답 DTO가 레코드로 구현되어 있어 조회 타입마다 메소드를 구현했는데, 구현체를 일반 클래스로 바꿔 공통 필드를 상속받게 변경할지를 고려해 볼 것.
    public Page<CourseWithFullScheduleResponse> findAllCoursesModeFull(
            String title, String profName, Long majorId, Integer grade, Integer semester,
            SecurityUser principal, Pageable pageable) {
        Long universityId = getUnivIdFromPrincipal(principal);
        return courseRepository
                .findWithFilters(universityId, title, profName, majorId, grade, semester, pageable)
                .map(CourseWithFullScheduleResponse::from);
    }

    /**
     * 주어진 필터링/페이지네이션 정보를 바탕으로, 인증되어 있는 유저의 소속 대학에서 제공되고 있는 강의 목록을 반환한다(수강 신청시).
     *
     * @param title     강의 이름 필터링 문자열
     * @param profName  교수 이름 필터링 문자열
     * @param principal 인증되어있는 유저
     * @param pageable  페이지네이션 정보
     * @return 조회된 강의에 해당하는 {@link CourseEnrollmentResponse} DTO가 담긴 {@link Page} 오브젝트
     */
    public Page<CourseEnrollmentResponse> findAllCoursesModeEnroll(
            String title, String profName, Long majorId, Integer grade, Integer semester,
            SecurityUser principal, Pageable pageable) {

        Long authUserUnivId = getUnivIdFromPrincipal(principal);
        return courseRepository
                .findWithFilters(authUserUnivId, title, profName, majorId, grade, semester, pageable)
                .map(CourseEnrollmentResponse::from);
    }

    /**
     * 주어진 필터링/페이지네이션 정보를 바탕으로, 인증되어 있는 유저의 소속 대학에서 제공되고 있는 강의 목록을 반환한다(강의 목록 조회시).
     *
     * @param title     강의 이름 필터링 문자열
     * @param profName  교수 이름 필터링 문자열
     * @param principal 인증되어있는 유저
     * @param pageable  페이지네이션 정보
     * @return 조회된 강의에 해당하는 {@link CourseResponse} DTO가 담긴 {@link Page} 오브젝트
     */
    public Page<CourseResponse> findAllCoursesModeCatalog(
            String title, String profName, Long majorId, Integer grade, Integer semester,
            SecurityUser principal, Pageable pageable) {

        Long authUserUnivId = getUnivIdFromPrincipal(principal);
        return courseRepository
                .findWithFilters(authUserUnivId, title, profName, majorId, grade, semester, pageable)
                .map(CourseResponse::from);
    }

    /**
     * 주어진 필터링/페이지네이션 정보를 바탕으로, 인증되어 있는 유저의 소속 대학에서 제공되고 있는 강의 목록을 반환한다(강의 목록 조회시).
     *
     * @param title     강의 이름 필터링 문자열
     * @param profName  교수 이름 필터링 문자열
     * @param principal 인증되어있는 유저
     * @param pageable  페이지네이션 정보
     * @return 조회된 강의에 해당하는 {@link TimetableCourseResponse} DTO가 담긴 {@link Page} 오브젝트
     */
    public Page<TimetableCourseResponse> findAllCoursesModeTimetable(
            String title, String profName, Long majorId, Integer grade, Integer semester,
            SecurityUser principal, Pageable pageable) {

        Long authUserUnivId = getUnivIdFromPrincipal(principal);
        return courseRepository
                .findWithFilters(authUserUnivId, title, profName, majorId, grade, semester, pageable)
                .map(TimetableCourseResponse::from);
    }

    /**
     * 주어진 {@link SecurityUser}로부터 소속 대학 ID를 추출한다.
     *
     * @param principal 대학 ID를 추출할 인증 유저 정보
     * @return 대학 ID
     */
    // TODO: 관리자가 강의 목록을 조회했을때 대응 추가
    // TODO: 회원 도메인 변경으로 인한 리팩토링 고려
    private Long getUnivIdFromPrincipal(SecurityUser principal) {
        GrantedAuthority authRole = principal.getAuthorities().stream().findFirst().orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "인증정보에서 권한을 불러오지 못했습니다.")
        );
        return switch (authRole.getAuthority()) {
            case "ROLE_STUDENT" -> studentRepository.findById(principal.getId()).orElseThrow(
                            () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "인증된 학생이 존재하지 않습니다.")
                    )
                    .getUniversity().getId();
            case "ROLE_PROFESSOR" -> professorRepository.findById(principal.getId()).orElseThrow(
                            () -> new UnihubException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "인증된 교수가 존재하지 않습니다.")
                    )
                    .getUniversity().getId();
            default ->
                    throw new UnihubException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "유저 권한 처리에 실패했습니다.");
        };
    }

    /**
     * 주어진 사번에 해당하는 교수의 스케줄이 주어진 스케줄과 겹치는지 검증.
     *
     * @param cs     검증할 스케줄
     * @param profId 검증할 교수의 사번
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    // TODO: 교수 정보를 사번 대신 고유 ID로 조회하게 변경. CourseSchedule의 수정도 필요할 것으로 사료됨.
    private boolean doesProfScheduleConflict(List<CourseScheduleDto> cs, String profId) {
        return cs.stream().anyMatch(csd -> courseScheduleRepository.existsByProfEmpIdAndDayOfWeek(
                profId,
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())
        ));
    }

    /**
     * 강의의 스케줄을 제외하고, 주어진 사번에 해당하는 교수의 스케줄이 주어진 스케줄과 겹치는지 검증.
     *
     * @param cs     검증할 스케줄
     * @param profId 검증할 교수의 사번
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    private boolean doesProfScheduleConflictExcludingCourse(List<CourseScheduleDto> cs, String profId, Long courseId) {
        return cs.stream().anyMatch(csd -> courseScheduleRepository.existsByProfEmpIdAndDayOfWeekExcludingCourse(
                profId,
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()),
                courseId
        ));
    }

    /**
     * 주어진 장소의 스케줄이 주어진 스케줄과 겹치는지 검증.
     *
     * @param cs       검증할 스케줄
     * @param univId   검증할 장소의 대학 ID
     * @param location 검증할 장소
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    private boolean doesLocationScheduleConflict(List<CourseScheduleDto> cs, Long univId, String location) {
        return cs.stream().anyMatch(csd -> courseScheduleRepository.existsByUnivIdAndLocationAndDayOfWeek(
                univId,
                location,
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())
        ));
    }

    /**
     * 주어진 장소의 스케줄이 주어진 스케줄과 겹치는지 검증.
     *
     * @param cs       검증할 스케줄
     * @param univId   검증할 장소의 대학 ID
     * @param location 검증할 장소
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    private boolean doesLocationScheduleConflictExcludingCourse(List<CourseScheduleDto> cs, Long univId, String location, Long courseId) {
        return cs.stream().anyMatch(csd -> courseScheduleRepository.existsByUnivIdAndLocationAndDayOfWeekExcludingCourse(
                univId,
                location,
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()),
                courseId
        ));
    }
}
