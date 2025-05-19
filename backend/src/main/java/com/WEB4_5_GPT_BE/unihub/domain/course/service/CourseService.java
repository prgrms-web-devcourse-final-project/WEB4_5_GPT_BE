package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.dto.*;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseScheduleRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.ProfessorProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.exception.MajorNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.university.exception.UniversityNotFoundException;
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

    private final EnrollmentRepository enrollmentRepository;

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
        University u = universityRepository.findByName(courseRequest.university()).orElseThrow(
                UniversityNotFoundException::new
        );
        Major m = majorRepository.findByUniversityIdAndName(u.getId(), courseRequest.major()).orElseThrow(
                MajorNotFoundException::new
        );
        if (doesLocationScheduleConflict(courseRequest.schedule(), u.getId(), courseRequest.location())) {
            throw new LocationScheduleConflictException();
        }
        Professor p = courseRequest.employeeId() == null ?
                null :
                professorRepository.findByUniversityIdAndEmployeeId(u.getId(), courseRequest.employeeId()).orElseThrow(
                        ProfessorProfileNotFoundException::new
                );
        if (p != null && doesProfScheduleConflict(courseRequest.schedule(), u.getId(), p.getEmployeeId())) {
            throw new ProfessorScheduleConflictException();
        }
        Course res = courseRequest.toEntity(m, 0, p);
        return CourseWithFullScheduleResponse.from(courseRepository.save(res));
    }

    public CourseWithFullScheduleResponse createCourse(CourseWithOutUrlRequest req, MultipartFile file) {
        String url = null;
        try {
            if (file != null && !file.isEmpty()) {
                url = s3Service.upload(file);
            }
            CourseRequest courseRequest = req.withCoursePlanAttachment(url);
            return createCourse(courseRequest); // 기존 createCourse(CourseRequest) 호출
        } catch (IOException e) {
            throw new FileUploadException();
        } catch (UnihubException ex) {
            deleteS3AttachmentIfExistsAndLog(url); // 실패 시 업로드 롤백
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
                CourseNotFoundException::new);
        University u = universityRepository.findByName(courseRequest.university()).orElseThrow(
                UniversityNotFoundException::new
        );
        Major m = majorRepository.findByUniversityIdAndName(u.getId(), courseRequest.major()).orElseThrow(
                MajorNotFoundException::new
        );
        if (doesLocationScheduleConflictExcludingCourse(courseRequest.schedule(), u.getId(), courseRequest.location(), courseId)) {
            throw new LocationScheduleConflictException();
        }
        Professor p = courseRequest.employeeId() == null ?
                null :
                professorRepository.findByUniversityIdAndEmployeeId(u.getId(), courseRequest.employeeId()).orElseThrow(
                        ProfessorProfileNotFoundException::new
                );
        if (p != null && doesProfScheduleConflictExcludingCourse(courseRequest.schedule(), u.getId(), p.getEmployeeId(), courseId)) {
            throw new ProfessorScheduleConflictException();
        }
        Course res = courseRequest.toEntity(m, orig.getEnrolled(), p);
        res.setId(orig.getId());

        // 기존 강의의 스케줄 s3 삭제, 실패 시에도 강의 수정은 정상 진행되도록 구현하고 log에 남김
        deleteS3AttachmentIfExistsAndLog(orig.getCoursePlanAttachment());

        return CourseWithFullScheduleResponse.from(courseRepository.save(res));
    }

    /**
     * 주어진 강의 정보를 주어진 ID에 해당하는 강의 위에 덮어쓴다.
     *
     * @param courseId 덮어쓰고자 하는 강의의 ID
     * @param req      덮어쓸 정보
     * @param file     강의계획서 파일
     * @return 입력한 file을 업로드 및 강의계획서 URL을 업데이트하여 기존 updateCourse(courseId, courseRequest) 호출
     */
    public CourseWithFullScheduleResponse updateCourse(Long courseId, CourseWithOutUrlRequest req, MultipartFile file) {
        String url = null;
        try {
            if (file != null && !file.isEmpty()) {
                url = s3Service.upload(file); // 사용자가 등록한 파일이 존재한다면 s3 업로드 url 발급
            }
            CourseRequest courseRequest = req.withCoursePlanAttachment(url);
            return updateCourse(courseId, courseRequest); // 기존 updateCourse(courseId, courseRequest) 호출
        } catch (IOException e) {
            throw new FileUploadException();
        } catch (UnihubException ex) {
            deleteS3AttachmentIfExistsAndLog(url); // 실패 시 업로드 롤백
            throw ex;
        }
    }

    private void deleteS3AttachmentIfExistsAndLog(String attachmentUrl) {
        if (attachmentUrl != null) {
            try {
                s3Service.deleteByUrl(attachmentUrl);
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (but ignoring): {}", attachmentUrl, e);
            }
        }
    }

    /**
     * 주어진 ID에 해당하는 강의를 삭제한다.
     *
     * @param courseId 삭제하고자 하는 강의의 ID
     */
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        // 수강신청이 하나라도 있으면 삭제 금지
        if (enrollmentRepository.existsByCourseId(courseId)) {
            throw new CourseDeletionException();
        }
        // s3에 업로드된 강의계획서 파일 삭제, 실패 시에도 강의 삭제는 정상 진행되도록 구현하고 log 남김
        deleteS3AttachmentIfExistsAndLog(course.getCoursePlanAttachment());

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
     * 주어진 필터링/페이지네이션 정보를 바탕으로, 인증되어 있는 유저의 소속 대학에서 제공되고 있는 강의 목록을 반환한다(시간표 조회시).
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
     * 주어진 {@link SecurityUser}로부터 소속 대학 ID를 추출한다. 소속 대학이 없을 경우, 예외를 던진다.
     *
     * @param principal 대학 ID를 추출할 인증 유저 정보
     * @return 대학 ID
     */
    // TODO: 회원 도메인 변경으로 인한 리팩토링 고려
    private Long getUnivIdFromPrincipal(SecurityUser principal) {
        GrantedAuthority authRole = principal.getAuthorities().stream().findFirst().orElseThrow(
                () -> new UnihubException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "인증정보에서 권한을 불러오지 못했습니다.")
        );
        return switch (authRole.getAuthority()) {
            case "ROLE_STUDENT" -> studentRepository.findById(principal.getId()).orElseThrow(
                            () -> new UnihubException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "인증된 학생이 존재하지 않습니다.")
                    )
                    .getUniversity().getId();
            case "ROLE_PROFESSOR" -> professorRepository.findById(principal.getId()).orElseThrow(
                            () -> new UnihubException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "인증된 교수가 존재하지 않습니다.")
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
     * @param univId 검증할 교수의 소속 대학 ID
     * @param profId 검증할 교수의 사번
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    private boolean doesProfScheduleConflict(List<CourseScheduleDto> cs, Long univId, String profId) {
        return doScheduleSetsIntersect(
                cs,
                courseScheduleRepository.findByUniversityIdAndProfessorEmployeeId(univId, profId)
        );
    }

    /**
     * 강의의 스케줄을 제외하고, 주어진 사번에 해당하는 교수의 스케줄이 주어진 스케줄과 겹치는지 검증.
     *
     * @param cs     검증할 스케줄
     * @param univId 검증할 교수의 소속 대학 ID
     * @param profId 검증할 교수의 사번
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    private boolean doesProfScheduleConflictExcludingCourse(List<CourseScheduleDto> cs, Long univId, String profId, Long courseId) {
        return doScheduleSetsIntersect(
                cs,
                courseScheduleRepository.findByUniversityIdAndProfessorEmployeeIdExcludingCourse(univId, profId, courseId)
        );
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
        return doScheduleSetsIntersect(
                cs,
                courseScheduleRepository.findByUniversityIdAndLocation(univId, location)
        );
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
        return doScheduleSetsIntersect(
                cs,
                courseScheduleRepository.findByUniversityIdAndLocationExcludingCourse(univId, location, courseId)
        );
    }

    /**
     * 두 스케줄 목록 사이에 겹치는 스케줄이 있는지 검증.
     * @param proposed 검증할 스케줄 목록
     * @param existing 기존 스케줄 목록
     * @return 스케줄이 겹치면 {@code true}, 아닐시 {@code false}
     */
    private boolean doScheduleSetsIntersect(List<CourseScheduleDto> proposed, List<CourseSchedule> existing) {
        return existing.stream()
                .anyMatch(e ->
                        proposed.stream()
                                .anyMatch(p ->
                                    {
                                        Boolean a = LocalTime.parse(p.startTime()).compareTo(e.getStartTime()) <= 0;
                                        Boolean b = e.getStartTime().compareTo(LocalTime.parse(p.endTime())) < 0;
                                        Boolean c = LocalTime.parse(p.startTime()).compareTo(e.getEndTime()) < 0;
                                        Boolean d = e.getEndTime().compareTo(LocalTime.parse(p.endTime())) <= 0;
                                        return ((a && b) || (c && d)) && e.getDay() == p.day();
                                    }));
    }
}
