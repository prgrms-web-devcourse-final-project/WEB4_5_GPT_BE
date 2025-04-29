package com.WEB4_5_GPT_BE.unihub.domain.admin.service;

import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.AdminInviteRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.EnrollmentPeriodRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.ProfessorApprovalRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.EnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

  @Mock
  private MemberRepository memberRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ProfessorProfileRepository professorProfileRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("교직원 상태 변경 테스트")
    void changeProfessorStatusTest() {
        // given
        Long memberId = 1L;
        ProfessorApprovalRequest request = new ProfessorApprovalRequest(ApprovalStatus.APPROVED);

        ProfessorProfile professorProfile = mock(ProfessorProfile.class);
        when(professorProfileRepository.findById(memberId)).thenReturn(Optional.of(professorProfile));

        // when
        adminService.changeProfessorStatus(memberId, request);

        // then
        verify(professorProfile).setApprovalStatus(ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("교직원 상태 변경 실패 - 존재하지 않는 교직원")
    void changeProfessorStatusFailTest() {
        // given
        Long memberId = 1L;
        ProfessorApprovalRequest request = new ProfessorApprovalRequest(ApprovalStatus.APPROVED);

        when(professorProfileRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.changeProfessorStatus(memberId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 교직원이 존재하지 않습니다");
    }

    @Test
    @DisplayName("수강신청 기간 등록 테스트")
    void createEnrollmentPeriodTest() {
        // given
        EnrollmentPeriodRequest request =
                new EnrollmentPeriodRequest(1L, 1, "2025-05-01", "2025-05-10");

        University university = University.builder().id(1L).name("테스트 대학").build();
        when(universityRepository.getReferenceById(1L)).thenReturn(university);

        EnrollmentPeriod savedPeriod =
                EnrollmentPeriod.builder()
                        .id(1L)
                        .university(university)
                        .grade(1)
                        .startDate(LocalDate.parse("2025-05-01"))
                        .endDate(LocalDate.parse("2025-05-10"))
                        .build();
        when(courseRepository.save(any(EnrollmentPeriod.class))).thenReturn(savedPeriod);

        // when
        EnrollmentPeriodResponse response = adminService.createEnrollmentPeriod(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.universityName()).isEqualTo("테스트 대학");
        assertThat(response.grade()).isEqualTo(1);
        assertThat(response.startDate()).isEqualTo(LocalDate.parse("2025-05-01"));
        assertThat(response.endDate()).isEqualTo(LocalDate.parse("2025-05-10"));
    }

    @Test
    @DisplayName("수강신청 기간 등록 실패 - 시작일이 종료일보다 늦은 경우")
    void createEnrollmentPeriodFailTest() {
        // given
        EnrollmentPeriodRequest request =
                new EnrollmentPeriodRequest(1L, 1, "2025-05-10", "2025-05-01");

        // when & then
        assertThatThrownBy(() -> adminService.createEnrollmentPeriod(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료일자는 시작일자보다 커야합니다");
    }

    @Test
    @DisplayName("수강신청 기간 수정 테스트")
    void updateEnrollmentPeriodTest() {
        // given
        Long periodId = 1L;
        EnrollmentPeriodRequest request =
                new EnrollmentPeriodRequest(2L, 2, "2025-06-01", "2025-06-10");

        University oldUniversity = University.builder().id(1L).name("이전 대학").build();
        University newUniversity = University.builder().id(2L).name("새 대학").build();

        EnrollmentPeriod existingPeriod =
                EnrollmentPeriod.builder()
                        .id(periodId)
                        .university(oldUniversity)
                        .grade(1)
                        .startDate(LocalDate.parse("2025-05-01"))
                        .endDate(LocalDate.parse("2025-05-10"))
                        .build();

        when(courseRepository.findById(periodId)).thenReturn(Optional.of(existingPeriod));
        when(universityRepository.getReferenceById(2L)).thenReturn(newUniversity);

        // when
        EnrollmentPeriodResponse response = adminService.updateEnrollmentPeriod(periodId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(periodId);
        assertThat(response.universityName()).isEqualTo("새 대학");
        assertThat(response.grade()).isEqualTo(2);
        assertThat(response.startDate()).isEqualTo(LocalDate.parse("2025-06-01"));
        assertThat(response.endDate()).isEqualTo(LocalDate.parse("2025-06-10"));
    }

    @Test
    @DisplayName("수강신청 기간 삭제 테스트")
    void deleteEnrollmentPeriodTest() {
        // given
        Long periodId = 1L;
        when(courseRepository.existsById(periodId)).thenReturn(true);

        // when
        adminService.deleteEnrollmentPeriod(periodId);

        // then
        verify(courseRepository).deleteById(periodId);
    }

    @Test
    @DisplayName("수강신청 기간 삭제 실패 - 존재하지 않는 기간")
    void deleteEnrollmentPeriodFailTest() {
        // given
        Long periodId = 1L;
        when(courseRepository.existsById(periodId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.deleteEnrollmentPeriod(periodId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 수강신청 기간이 존재하지 않습니다");
    }

    @Test
    @DisplayName("관리자 초대 테스트")
    void inviteAdminTest() {
        // given
        AdminInviteRequest request = new AdminInviteRequest("관리자", "admin@example.com");
        when(memberRepository.existsByEmail("admin@example.com")).thenReturn(false);

        // when
        adminService.inviteAdmin(request);

        // then
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("관리자 초대 실패 - 이미 존재하는 이메일")
    void inviteAdminFailTest() {
        // given
        AdminInviteRequest request = new AdminInviteRequest("관리자", "admin@example.com");
        when(memberRepository.existsByEmail("admin@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminService.inviteAdmin(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 이메일입니다");
    }
}
