package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.EnrollmentNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.EnrollmentPeriodClosedException;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.EnrollmentPeriodNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository; // 수강신청 Repository
    private final EnrollmentPeriodRepository enrollmentPeriodRepository; // 수강신청 기간 Repository
    private final CourseRepository courseRepository;

    /**
     * 학생의 수강신청 내역을 조회하는 메서드입니다.
     *
     * @param student 로그인 인증된 학생 정보
     * @return 수강신청 내역에 해당하는 {@link MyEnrollmentResponse} DTO 리스트
     * */
    @Transactional
    public List<MyEnrollmentResponse> getMyEnrollmentList(Member student) {

        // student → StudentProfile
        StudentProfile profile = student.getStudentProfile();

        // 해당 학생의 수강신청 목록을 조회하고, DTO로 변환하여 반환
        return enrollmentRepository
                .findAllByStudent(profile)
                .stream()
                .map(MyEnrollmentResponse::from)
                .toList();
    }

    /**
     * 수강 취소 요청을 처리하는 메서드입니다.
     *
     * @param student  로그인 인증된 학생 정보
     * @param courseId 취소할 강좌의 ID
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     * @throws EnrollmentNotFoundException       수강신청 내역이 없는 경우
     */
    @Transactional
    public void cancelMyEnrollment(Member student, Long courseId) {
        // Member → StudentProfile 조회
        StudentProfile profile = student.getStudentProfile();

        // 수강 취소 가능 기간인지 검증
        ensureEnrollmentPeriodActive(profile);

        // 3) 취소하려는 강좌에 대한 수강 신청 정보 조회
        Enrollment enrollment = findEnrollment(profile.getId(), courseId);

        // 4) 수강 취소 완료
        enrollmentRepository.delete(enrollment);
    }

    /**
     * 수강신청, 취소 가능 기간인지 검증한다.
     * <p>
     * 1) 해당 학생의 (학교·연도·학년·학기) 수강신청 기간을 조회하고,
     * 2) 그 기간에 오늘이 포함되는지 검증한다.
     *
     * @param profile 학생의 프로필 정보
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     */
    private void ensureEnrollmentPeriodActive(StudentProfile profile) {
        // 오늘 날짜 조회
        LocalDate today = LocalDate.now();

        // 1) 학생 정보에 해당하는 수강신청 기간 조회
        EnrollmentPeriod period = findEnrollmentPeriod(profile, today);

        // 2) 조회된 수강신청 기간 내에 오늘(요청일자)이 포함되는지 검증
        validateWithinPeriod(period, today);
    }

    /**
     * 학생 프로필과 날짜를 기준으로 해당 학기 수강신청 기간을 조회한다.
     *
     * @param profile 학생 정보
     * @param today   요청 날짜
     * @return 학생 정보와 일치하는 수강 신청 기간 정보
     * @throws EnrollmentPeriodNotFoundException 기간 정보가 없는 경우
     */
    private EnrollmentPeriod findEnrollmentPeriod(StudentProfile profile, LocalDate today) {
        // (학교·연도·학년·학기)를 기준으로 수강신청 기간 조회
        return enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        profile.getUniversity().getId(),
                        today.getYear(),
                        profile.getGrade(),
                        profile.getSemester()
                )
                .orElseThrow(EnrollmentPeriodNotFoundException::new);
    }

    /**
     * 조회된 수강신청 기간에 오늘 날짜가 포함되는지 확인한다.
     *
     * @param period 조회된 EnrollmentPeriod
     * @param today  확인할 날짜
     * @throws EnrollmentPeriodClosedException 기간 외인 경우
     */
    private void validateWithinPeriod(EnrollmentPeriod period, LocalDate today) {
        if (today.isBefore(period.getStartDate()) || today.isAfter(period.getEndDate())) {
            throw new EnrollmentPeriodClosedException();
        }
    }

    /**
     * 학생 프로필 ID와 강좌 ID로 수강신청 내역을 조회한다.
     *
     * @param studentProfileId 학생 프로필의 ID
     * @param courseId         강좌의 ID
     * @return 조회된 Enrollment 엔티티
     * @throws EnrollmentNotFoundException 해당 강좌에 대한 수강신청 내역이 없는 경우
     */
    private Enrollment findEnrollment(Long studentProfileId, Long courseId) {
        return enrollmentRepository
                .findByCourseIdAndStudentId(courseId, studentProfileId)
                .orElseThrow(EnrollmentNotFoundException::new);
    }

    @Transactional
    public void enrollment(Member student, Long courseId) {
        // Member → StudentProfile 추출
        StudentProfile profile = student.getStudentProfile();

        // 수강 신청 가능 기간인지 검증
        ensureEnrollmentPeriodActive(profile);

        // 신청하려는 강좌 정보 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        // TODO: 정원 초과
        // TODO: 이미 신청한 과목
        // TODO: 시간표 충돌
        // TODO: 학점 한도 초과

        // 수강 신청 정보 생성
        Enrollment enrollment = Enrollment.builder()
                .student(profile)
                .course(course)
                .build();

        enrollmentRepository.save(enrollment);
    }
}
