package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.StudentProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentValidator {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentPeriodRepository enrollmentPeriodRepository;
    private final RedissonClient redisson;

    private final int MAXIMUM_CREDIT = 21; // 최대 학점 상수 (21학점)

    /**
     * 수강 신청이 가능한지 여러 예외 상황을 검증합니다.
     */
    @Transactional
    void ensureEnrollmentAllowed(Long studentId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        Student profile = studentRepository.findById(studentId)
                .orElseThrow(StudentProfileNotFoundException::new);

        ensureEnrollmentPeriodActive(profile); // 수강 신청 가능 기간인지 검증

        ensureNotAlreadyEnrolled(profile, course); // 동일강좌 중복 신청 방지

        List<Enrollment> enrollmentList = enrollmentRepository.findAllByStudent(profile); // 학생 기존 수강 신청 내역

        ensureCreditLimitNotExceeded(enrollmentList, course); // 최대 학점 (21)을 초과하여 신청하는지 검증
        ensureNoScheduleConflict(enrollmentList, course); // 기존에 신청했던 강좌들과 새 수업 시간표가 겹치는지 검증
    }

    /**
     * 학생이 동일 강좌를 이미 신청하지 않았는지 확인한다.
     *
     * @throws DuplicateEnrollmentException 이미 신청되어 있으면
     */
    private void ensureNotAlreadyEnrolled(Student profile, Course course) {
        Long courseId = course.getId();
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, profile.getId())) {
            throw new DuplicateEnrollmentException();
        }
    }

    /**
     * 학생의 기존 신청 학점 + 새 강좌 학점이
     * 최대 허용 학점(MAXIMUM_CREDIT)을 넘지 않는지 확인한다.
     *
     * @throws CreditLimitExceededException 학점 초과 시
     */
    private void ensureCreditLimitNotExceeded(List<Enrollment> enrollmentList, Course course) {
        // 수강 신청한 과목의 총 학점 계산
        Integer totalCredit = enrollmentList.stream()
                .mapToInt(e -> e.getCourse().getCredit())
                .sum();

        // 현재 학생 정보에 학점 관련 칼럼 존재하지 않아 별도의 21학점 상수로 처리
        if (totalCredit + course.getCredit() > MAXIMUM_CREDIT) {
            throw new CreditLimitExceededException();
        }
    }

    /**
     * 기존에 수강신청한 과목들의 시간표와 새로 신청할 과목의 시간표가 겹치는지 확인합니다.
     *
     * @param enrollmentList 수강신청 완료한 과목 목록
     * @param newCourse      새로 신청할 과목
     * @throws ScheduleConflictException 시간표가 겹치는 경우
     */
    private void ensureNoScheduleConflict(List<Enrollment> enrollmentList, Course newCourse) {
        // 1) 이미 신청된 강좌의 시간표 정보를 모두 가져온다
        List<CourseSchedule> enrolledSchedules = collectSchedules(enrollmentList);

        // 2) 기존 강좌와 새 강좌의 시간표가 겹치는지 확인한다
        newCourse.getSchedules()
                .forEach(newSchedule -> checkScheduleConflict(newSchedule, enrolledSchedules));
    }

    /**
     * 내 수강신청 과목들의 시간표 정보를 가져와 List 형태로 반환한다.
     *
     * @param enrollments 수강신청 목록
     * @return 수강신청 과목들의 시간표 정보
     */
    private List<CourseSchedule> collectSchedules(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(Enrollment::getCourse)
                .map(Course::getSchedules)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 새 스케줄(newSchedule) 이 기존 스케줄 목록(enrolledSchedules) 중 하나라도 겹치면 예외를 반환합니다.
     *
     * @param newSchedule       새로 추가할 강의 스케줄
     * @param enrolledSchedules 이미 수강신청한 강의 스케줄 목록
     * @throws ScheduleConflictException 스케줄이 겹치는 경우
     */
    private void checkScheduleConflict(CourseSchedule newSchedule, List<CourseSchedule> enrolledSchedules) {
        enrolledSchedules.stream()
                .filter(enrolledSchedule -> newSchedule.getDay().equals(enrolledSchedule.getDay()))
                .filter(enrolledSchedule -> isTimeOverlap(newSchedule, enrolledSchedule))
                .findAny()
                .ifPresent(enrolledSchedule -> {
                    throw new ScheduleConflictException();
                });
    }

    /**
     * 두 스케줄의 강의 시간이 겹치는지 확인하는 메서드입니다
     */
    private boolean isTimeOverlap(CourseSchedule a, CourseSchedule b) {
        return a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
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
    void ensureEnrollmentPeriodActive(Student profile) {
        // 오늘 날짜 조회
        LocalDate today = LocalDate.now();

        // 1) 학생 정보에 해당하는 수강신청 기간 조회
        EnrollmentPeriod period = findEnrollmentPeriod(profile, today);

        // 2) 조회된 수강신청 기간 내에 오늘(요청일자)이 포함되는지 검증
        if (!isWithinPeriod(period, today)) {
            throw new EnrollmentPeriodClosedException();
        }
    }

    /**
     * 학생 프로필과 날짜를 기준으로 해당 학기 수강신청 기간을 조회한다.
     *
     * @param profile 학생 정보
     * @param today   요청 날짜
     * @return 학생 정보와 일치하는 수강 신청 기간 정보
     * @throws EnrollmentPeriodNotFoundException 기간 정보가 없는 경우
     */
    private EnrollmentPeriod findEnrollmentPeriod(Student profile, LocalDate today) {
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
     * 오늘 날짜가 수강 신청 시작일 이후이고 종료일 이전인지 확인합니다.
     *
     * @param period 조회된 EnrollmentPeriod
     * @param today  확인할 날짜
     * @return true: 오늘 날짜가 수강신청 기간 내에 포함됨
     */
    boolean isWithinPeriod(EnrollmentPeriod period, LocalDate today) {
        return period.getStartDate().isBefore(today)
                && period.getEndDate().isAfter(today);
    }

    /**
     * Redis 카운터를 가져와 남은 자리가 강좌의 남은 좌석이 1개 이상인지 확인한다.
     *
     * @param courseId 강의 ID
     * @throws CourseCapacityExceededException 정원 초과 시
     * @throws CourseNotFoundException         강좌가 존재하지 않는 경우
     */
    void ensureCapacityAvailable(Long courseId) {
        // 강좌가 없으면 예외처리
        courseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);

        long capacity = redisson.getAtomicLong("course:" + courseId + ":capacity").get();   // 전체 정원
        long enrolled = redisson.getAtomicLong("course:" + courseId + ":enrolled").get();   // 현재까지 예약된 수

        if (enrolled >= capacity) {
            throw new CourseCapacityExceededException();
        }
    }
}