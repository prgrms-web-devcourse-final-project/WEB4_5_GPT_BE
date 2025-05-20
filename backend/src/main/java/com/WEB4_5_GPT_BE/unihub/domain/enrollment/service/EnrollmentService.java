package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.dto.TimetableCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.StudentEnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.EnrollmentCommand;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.EnrollmentDuplicateChecker;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel.EnrollmentCancelCommand;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel.EnrollmentCancelDuplicateChecker;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.StudentProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 수강 신청, 취소, 내 수강목록 조회 등의
 * 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository; // 수강신청 Repository
    private final EnrollmentPeriodRepository enrollmentPeriodRepository; // 수강신청 기간 Repository
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository; // 강좌 Repository

    private final RedissonClient redisson; // 동시성 처리를 위한 RedissonClient
    private final EnrollmentDuplicateChecker enrollmentCancelDuplicateCheckerDuplicateChecker; // 중복 큐 삽입 방지

    private final RBlockingQueue<EnrollmentCommand> enrollQueue; // 수강신청 요청을 저장하는 Queue
    private final RBlockingQueue<EnrollmentCancelCommand> cancelQueue; // 수강신청 취소 요청을 저장하는 Queue

    private final int MAXIMUM_CREDIT = 21; // 최대 학점 상수 (21학점)
    private final EnrollmentQueueService enrollmentQueueService; // 수강신청 대기열 큐 TODO: 테스트 후 제거
    private final EnrollmentCancelDuplicateChecker enrollmentCancelDuplicateChecker;

    /**
     * 학생의 수강신청 내역을 조회하는 메서드입니다.
     *
     * @param student 로그인 인증된 학생 정보
     * @return 수강신청 내역에 해당하는 {@link MyEnrollmentResponse} DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyEnrollmentResponse> getMyEnrollmentList(Member student) {

        // student → StudentProfile
        Student profile = studentRepository.findById(student.getId())
                .orElseThrow(StudentProfileNotFoundException::new);

        // 해당 학생의 수강신청 목록을 조회하고, DTO로 변환하여 반환
        return enrollmentRepository
                .findAllByStudent(profile)
                .stream()
                .map(MyEnrollmentResponse::from)
                .toList();
    }

    /**
     * 시간표 등록용 내 수강 목록 조회
     *
     * @param student 로그인 인증된 학생 정보
     * @return 수강신청 내역에 해당하는 {@link MyEnrollmentResponse} DTO 리스트
     */
    @Transactional
    public List<TimetableCourseResponse> getMyEnrollmentsForTimetable(SecurityUser student, int year, Integer semester) {

        // student → StudentProfile
        Student profile = studentRepository.getReferenceById(student.getId());

        ensureEnrollmentPeriodActive(profile);

        // 해당 학생의 수강신청 목록을 조회하고, DTO로 변환하여 반환
        return enrollmentRepository
                .findAllByStudent(profile)
                .stream()
                .filter(it -> it.getCourse().getSemester().equals(semester) && it.getCourse().getCreatedAt().getYear() == year)
                .map(TimetableCourseResponse::from)
                .toList();
    }

    /**
     * 수강 취소 요청을 처리하는 메서드입니다.
     *
     * @param studentId  로그인 인증된 학생 정보
     * @param courseId 취소할 강좌의 ID
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     * @throws EnrollmentNotFoundException       수강신청 내역이 없는 경우
     */
    @Transactional(readOnly = true)
    public void cancelMyEnrollment(Long studentId, Long courseId) {

        enrollmentQueueService.addToQueue(String.valueOf(studentId)); // TODO: 테스트 후 제거

        // Member → StudentProfile 조회
        Student profile = studentRepository.findById(studentId)
                .orElseThrow(StudentProfileNotFoundException::new);

        // 수강 취소 가능 기간인지 검증
        ensureEnrollmentPeriodActive(profile);

        // 2) 중복 취소 커맨드 방지
        enrollmentCancelDuplicateChecker.markEnqueuedIfAbsent(studentId, courseId);

        // 3) 실제 취소 자리 공석 + 큐에 커맨드 추가
        tryDecrementAndEnqueueCancel(studentId, courseId);
    }


    /**
     * Redis 큐에 수강취소 명령(EnrollmentCancelCommand)을 추가하여 순차적으로 수강취소 처리합니다.
     * <p>
     * 1) AtomicLong을 사용해 현재 수강인원(enrolled)을 1 감소시킵니다.
     * 2) 감소된 값이 0보다 작으면, 감소를 원복하고 플래그를 삭제한 뒤
     * CannotCancelException을 던집니다.
     * 3) 수강취소 명령을 Redis 큐에 enqueue하고,
     * 실패하는 경우 역시 감소된 카운터와 플래그를 원복합니다.
     *
     * @param studentId 학생 ID
     * @param courseId  강좌 ID
     * @throws CannotCancelException 취소 가능한 수강신청 내역이 없을 때 발생
     */
    private void tryDecrementAndEnqueueCancel(Long studentId, Long courseId) {
        // Redis 수강인원 카운터 키: course:{courseId}:enrolled
        String enrolledKey = "course:" + courseId + ":enrolled";
        RAtomicLong enrolled = redisson.getAtomicLong(enrolledKey);

        // 중복 enqueue 방지 플래그 키: cancel:queued:{studentId}:{courseId}
        String flagKey = "cancel:queued:" + studentId + ":" + courseId;

        // 1) 수강가능인원 반납 시도: enrolled 값을 1 감소
        long newCount = enrolled.decrementAndGet();
        if (newCount < 0) {
            // 2) 감소된 값이 음수이면 반납 복구 및 플래그 롤백 후 예외 발생
            enrolled.incrementAndGet();
            redisson.getBucket(flagKey).delete();
            throw new CannotCancelException();
        }

        try {
            // 3) Redis 큐에 수강취소 명령 추가
            cancelQueue.add(new EnrollmentCancelCommand(studentId, courseId));
        } catch (Exception ex) {
            // 4) enqueue 실패 시 자리 반납 롤백 및 플래그 롤백
            enrolled.incrementAndGet();
            redisson.getBucket(flagKey).delete();
            throw ex;
        }
    }

    /**
     * 수강신청, 취소 가능 기간인지 검증한다.
     *
     * 1) 해당 학생의 (학교·연도·학년·학기) 수강신청 기간을 조회하고,
     * 2) 그 기간에 오늘이 포함되는지 검증한다.
     *
     * @param profile 학생의 프로필 정보
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     */
    private void ensureEnrollmentPeriodActive(Student profile) {
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
    private boolean isWithinPeriod(EnrollmentPeriod period, LocalDate today) {
        return period.getStartDate().isBefore(today)
                && period.getEndDate().isAfter(today);
    }

    /**
     * 학생 프로필 ID와 강좌 ID로 수강신청 내역을 조회한다.
     *
     * @param studentId 학생 ID
     * @param courseId  강좌 ID
     * @return 조회된 Enrollment 엔티티
     * @throws EnrollmentNotFoundException 해당 강좌에 대한 수강신청 내역이 없는 경우
     */
    private Enrollment findEnrollment(Long studentId, Long courseId) {
        return enrollmentRepository
                .findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(EnrollmentNotFoundException::new);
    }

    /**
     * 비동기 수강 신청을 처리하는 메서드입니다.
     *
     * 여러 예외 상황을 검증한 후 수강 신청을 진행합니다.
     * redis의 AtomicLong을 사용하여 수강인원 카운트를 관리하며 원자성을 보장합니다.
     * 실제 DB 저장에 저장하는 로직은 redisson queue를 통해 순차적으로 처리합니다.
     * DB 저장은 별도의 트랜잭션으로 처리하고 사용자의 요청을 redis의 AtomicLong을 통해 바로바로 처리하기 때문에 빠른 응답을 보장합니다.
     *
     * @param studentId 로그인 인증된 학생 ID
     * @param courseId  신청할 강좌의 ID
     * @throws CourseNotFoundException           강좌 정보가 없는 경우
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     * @throws CourseCapacityExceededException   정원 초과 시
     * @throws DuplicateEnrollmentException      동일 강좌 중복 신청 시
     * @throws CreditLimitExceededException      최대 학점 초과 시
     * @throws ScheduleConflictException         기존 신청한 강좌와 시간표가 겹치는 경우
     */
    @Transactional(readOnly = true)
    public void enrollment(Long studentId, Long courseId) {

        // 동시성 테스트를 위해 임시로 대기열 제거
        enrollmentQueueService.addToQueue(String.valueOf(studentId)); // TODO: 테스트 후 제거

        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        ensureCapacityAvailable(courseId); // 정원 초과 시 예외 처리

        // Member → StudentProfile 추출
        Student profile = studentRepository.findById(studentId)
                .orElseThrow(StudentProfileNotFoundException::new);

        ensureEnrollmentAllowed(profile, course); // 수강 신청이 가능한지 여러 예외상황 검증

        // 2) 동일학생+동일강좌 수강신청이 이미 큐에 들어가있는지 확인
        enrollmentCancelDuplicateCheckerDuplicateChecker.markEnqueuedIfAbsent(studentId, courseId);

        // 3) 실제 자리 확보 + 큐 등록
        tryReserveSeatAndEnqueue(studentId, courseId);

    }

    /**
     * 수강 신청이 가능한지 여러 예외 상황을 검증합니다.
     */
    private void ensureEnrollmentAllowed(Student profile, Course course) {
        ensureEnrollmentPeriodActive(profile); // 수강 신청 가능 기간인지 검증

        ensureNotAlreadyEnrolled(profile, course); // 동일강좌 중복 신청 방지

        List<Enrollment> enrollmentList = enrollmentRepository.findAllByStudent(profile); // 학생 기존 수강 신청 내역

        ensureCreditLimitNotExceeded(enrollmentList, course); // 최대 학점 (21)을 초과하여 신청하는지 검증
        ensureNoScheduleConflict(enrollmentList, course); // 기존에 신청했던 강좌들과 새 수업 시간표가 겹치는지 검증
    }

    /**
     * Redis 카운터를 가져와 남은 자리가 강좌의 남은 좌석이 1개 이상인지 확인한다.
     *
     * @param courseId 강의 ID
     * @throws CourseCapacityExceededException 정원 초과 시
     */
    private void ensureCapacityAvailable(Long courseId) {

        long capacity = redisson.getAtomicLong("course:" + courseId + ":capacity").get();   // 전체 정원
        long enrolled = redisson.getAtomicLong("course:" + courseId + ":enrolled").get();   // 현재까지 예약된 수

        if (enrolled >= capacity) {
            throw new CourseCapacityExceededException();
        }
    }

    /**
     * Redis의 AtomicLong을 통해 수강신청 자리 확보 후 Redis 큐에 수강신청 명령을 추가합니다.
     *
     * @param studentId 학생 ID
     * @param courseId  강좌 ID
     * @throws CourseCapacityExceededException 정원 초과 시 예외 발생
     */
    private void tryReserveSeatAndEnqueue(Long studentId, Long courseId) {
        // Redis 키 생성
        String enrolledKey = "course:" + courseId + ":enrolled";
        String capacityKey = "course:" + courseId + ":capacity";
        RAtomicLong enrolled = redisson.getAtomicLong(enrolledKey);
        RAtomicLong capacity = redisson.getAtomicLong(capacityKey);

        // 중복 enqueue 방지 플래그 키
        String flagKey = "enroll:queued:" + studentId + ":" + courseId;

        // 1) 자리 확보: 현재 enrolled 값을 1 증가시키고
        long newCount = enrolled.incrementAndGet();
        //    증가된 값이 capacity를 넘으면 복구 후 예외
        if (newCount > capacity.get()) {
            enrolled.decrementAndGet();                  // 자리 반납
            redisson.getBucket(flagKey).delete();        // enqueue 플래그 롤백
            throw new CourseCapacityExceededException(); // 정원 초과 예외
        }

        try {
            // 2) Redis 큐에 수강신청 명령 추가
            enrollQueue.add(new EnrollmentCommand(studentId, courseId));
        } catch (Exception e) {
            // 3) enqueue 중 에러 발생 시 복구
            enrolled.decrementAndGet();                  // 자리 반납
            redisson.getBucket(flagKey).delete();        // enqueue 플래그 롤백
            throw e;                                     // 예외 재던짐
        }
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
     * 학생의 수강신청 기간을 조회합니다.
     *
     * @param student 로그인 인증된 학생 정보
     * @return 수강신청 기간에 해당하는 {@link StudentEnrollmentPeriodResponse} DTO
     */
    @Transactional
    public StudentEnrollmentPeriodResponse getMyEnrollmentPeriod(Member student) {

        Student profile = studentRepository.findById(student.getId())
                .orElseThrow(StudentProfileNotFoundException::new); // 학생 프로필 정보

        University university = profile.getUniversity(); // 학생 소속 대학교 정보
        LocalDate today = LocalDate.now(); // 오늘 날짜

        // 1) 학생 정보에 해당하는 수강신청 기간 조회
        Optional<EnrollmentPeriod> opEnrollmentPeriod = enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        university.getId(), today.getYear(), profile.getGrade(), profile.getSemester()
                );

        // 2) 조회된 수강신청 기간 내에 오늘(요청일자)이 포함되는지 검증
        // 3) 수강신청 기간이 없거나 오늘 날짜가 포함되지 않으면 isEnrollmentOpen=false가 포함된 DTO 반환
        return opEnrollmentPeriod
                .map(period -> {
                    boolean isOpen = isWithinPeriod(period, today);
                    return StudentEnrollmentPeriodResponse.from(period, profile, isOpen);
                })
                .orElse(StudentEnrollmentPeriodResponse.notOpen());
    }
}
