package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.dto.TimetableCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.StudentEnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel.EnrollmentCancelCommand;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.enroll.EnrollmentCommand;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.StudentProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.global.concurrent.ConcurrencyGuard;
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
    private final EnrollmentValidator enrollmentValidator;

    private final RedissonClient redisson; // 동시성 처리를 위한 RedissonClient

    private final RBlockingQueue<EnrollmentCommand> enrollQueue; // 수강신청 요청을 저장하는 Queue
    private final RBlockingQueue<EnrollmentCancelCommand> cancelQueue; // 수강신청 취소 요청을 저장하는 Queue



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

        enrollmentValidator.ensureEnrollmentPeriodActive(profile);

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
     * @ConcurrencyGuard 학생id + 강의id 조합에 대한 분산락을 적용하여 중복 요청을 방지합니다.
     *
     * @param studentId  로그인 인증된 학생 정보
     * @param courseId 취소할 강좌의 ID
     * @throws EnrollmentPeriodNotFoundException 수강신청 기간 정보가 없는 경우
     * @throws EnrollmentPeriodClosedException   수강신청 기간 외 요청인 경우
     * @throws EnrollmentNotFoundException       수강신청 내역이 없는 경우
     */
    @ConcurrencyGuard(lockName = "student:cancel")
    public void cancelMyEnrollment(Long studentId, Long courseId) {

        // Member → StudentProfile 조회
        Student profile = studentRepository.findById(studentId)
                .orElseThrow(StudentProfileNotFoundException::new);

        // 수강 취소 가능 기간인지 검증
        enrollmentValidator.ensureEnrollmentPeriodActive(profile);

        // 수강 신청 내역이 있는지 조회 없다면 예외처리
        if (!enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new EnrollmentNotFoundException();
        }

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
     * 비동기 수강 신청을 처리하는 메서드입니다.
     *
     * 여러 예외 상황을 검증한 후 수강 신청을 진행합니다.
     * @ConcurrencyGuard 학생id + 강의id 조합에 대한 분산락을 적용하여 중복 요청을 방지합니다.
     * 2. redis의 INCR 명령어를 통해 수강신청 인원 원자성을 보장하며 Redisson의 AtomicLong을 통해 INCR를 적용합니다.
     * 실제 DB 저장에 저장하는 로직은 redisson queue를 통해 순차적으로 처리합니다.
     * DB 저장은 별도의 트랜잭션으로 처리하고 사용자의 요청을 redis의 AtomicLong을 통해 바로바로 처리하기 때문에 빠른 응답을 보장합니다.
     *`
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
    @ConcurrencyGuard(lockName = "student:enroll")
    public void enrollment(Long studentId, Long courseId) {
        // 정원 초과 시 예외 처리
        enrollmentValidator.ensureCapacityAvailable(courseId);

        // 수강 신청이 가능한지 여러 예외상황 검증
        enrollmentValidator.ensureEnrollmentAllowed(studentId, courseId);

        // 3) 실제 자리 확보 + 큐 등록
        tryReserveSeatAndEnqueue(studentId, courseId);
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
                    boolean isOpen = enrollmentValidator.isWithinPeriod(period, today);
                    return StudentEnrollmentPeriodResponse.from(period, profile, isOpen);
                })
                .orElse(StudentEnrollmentPeriodResponse.notOpen());
    }
}
