package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * 로그인된 Member 객체를 받아,
     * 그 학생의 수강신청 목록을 MyEnrollmentResponse DTO 로 매핑해 반환
     */
    @Transactional
    public List<MyEnrollmentResponse> getMyEnrollmentList(Member student) {

        // 1) Member → StudentProfile
        StudentProfile profile = student.getStudentProfile();

        // 2) 해당 학생의 수강신청 엔티티를 조회하고, DTO로 변환하여 반환
        return enrollmentRepository
                .findAllByStudent(profile)         // 학생 프로필로 Enrollment 조회
                .stream()
                .map(MyEnrollmentResponse::from)   // Enrollment → DTO 변환
                .toList();
    }
}
