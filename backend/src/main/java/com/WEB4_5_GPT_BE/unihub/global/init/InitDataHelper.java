package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseScheduleRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Admin;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.AdminRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.EmailService;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InitDataHelper {

    private final UniversityRepository universityRepository;
    private final MajorRepository majorRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final ProfessorRepository professorRepository;
    private final AdminRepository adminRepository;
    private final EmailService emailService;
    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentPeriodRepository enrollmentPeriodRepository;

    public University createUniversity(String name, String emailDomain) {
        return universityRepository.save(University.builder().name(name).emailDomain(emailDomain).build());
    }

    public Major createMajor(String name, University university) {
        return majorRepository.save(Major.builder().name(name).university(university).build());
    }

    public void createStudent(String email, String pw, String name, String studentCode, Long univId, Long majorId, Integer grade, Integer semester) {
        emailService.markEmailAsVerified(email);
        memberService.signUpStudent(new StudentSignUpRequest(email, pw, name, studentCode, univId, majorId, grade, semester, Role.STUDENT));
    }

    public Course createCourse(String title, Major major, String location,
                               Integer capacity, Integer enrolled, Integer credit, Professor professor,
                               Integer grade, Integer semester, String coursePlanAttachment) {
        return courseRepository.save(
                Course.builder()
                        .title(title)
                        .major(major)
                        .location(location)
                        .capacity(capacity)
                        .enrolled(enrolled)
                        .credit(credit)
                        .professor(professor)
                        .grade(grade)
                        .semester(semester)
                        .coursePlanAttachment(coursePlanAttachment)
                        .build());
    }

    public CourseSchedule createCourseScheduleAndAssociateWithCourse(Course course, DayOfWeek day, String startTime, String endTime) {
        CourseSchedule cs = courseScheduleRepository.save(
                CourseSchedule.builder()
                        .course(course)
                        .universityId(course.getMajor().getUniversity().getId())
                        .location(course.getLocation())
                        .professorProfileEmployeeId(course.getProfessor().getEmployeeId())
                        .day(day)
                        .startTime(LocalTime.parse(startTime))
                        .endTime(LocalTime.parse(endTime))
                        .build());
        course.getSchedules().add(cs);
        courseRepository.save(course);
        return cs;
    }

    public Member createProfessor(String email, String pw, String name, String empCode, Long univId, Long majorId, ApprovalStatus status) {
        emailService.markEmailAsVerified(email);
        memberService.signUpProfessor(new ProfessorSignUpRequest(email, pw, name, empCode, univId, majorId, Role.PROFESSOR));
        Professor professor = (Professor) memberRepository.findByEmail(email).orElseThrow();
        professor.setApprovalStatus(status);
        return professorRepository.save(professor);
    }

    public void createAdmin(String email, String pw, String name, PasswordEncoder encoder) {
        Admin admin = Admin.builder()
                .email(email)
                .password(encoder.encode(pw))
                .name(name)
                .build();
        adminRepository.save(admin);
    }

    public long countMembers() {
        return memberRepository.count();
    }

    public void clearAllMemberData() {
        memberRepository.deleteAll();
        majorRepository.deleteAll();
        universityRepository.deleteAll();
    }

    public void ensureProfessorApprovalStatus(String email, ApprovalStatus status) {
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        if (optionalMember.isEmpty()) return;

        Member member = optionalMember.get();

        if (!(member instanceof Professor)) return;

        Professor profile = (Professor) member;
        if (profile.getApprovalStatus() == null) {
            profile.setApprovalStatus(status);
            memberRepository.save(member);
        }
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email).get();
    }

    public void createEnrollment(Member student, Long courseId) {
        Student profile = (Student) student;
        Course course = courseRepository.findById(courseId).get();
        course.incrementEnrolled();

        Enrollment enrollment = Enrollment.builder()
                .student(profile)
                .course(course)
                .build();

        enrollmentRepository.save(enrollment);
        courseRepository.save(course);
    }

    public void createEnrollmentPeriod(
            University university, Integer year, Integer grade,
            Integer semester, LocalDate startDate, LocalDate endDate
    ) {
        EnrollmentPeriod period = EnrollmentPeriod.builder()
                .university(university)
                .year(year)
                .grade(grade)
                .semester(semester)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        enrollmentPeriodRepository.save(period);
    }

}
