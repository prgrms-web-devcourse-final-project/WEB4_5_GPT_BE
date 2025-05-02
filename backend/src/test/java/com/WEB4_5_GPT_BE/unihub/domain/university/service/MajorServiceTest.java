package com.WEB4_5_GPT_BE.unihub.domain.university.service;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.MajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MajorServiceTest {

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private MajorService majorService;

    @Test
    @DisplayName("모든 전공 조회 성공")
    void getAllMajors_success() {
        // given
        University university = University.builder().id(1L).name("서울대학교").build();
        Major major1 = Major.builder().id(1L).university(university).name("컴퓨터공학과").build();
        Major major2 = Major.builder().id(2L).university(university).name("수학과").build();
        List<Major> majors = Arrays.asList(major1, major2);

        when(majorRepository.findAll()).thenReturn(majors);

        // when
        List<MajorResponse> result = majorService.getAllMajors();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("컴퓨터공학과");
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("수학과");

        verify(majorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("대학별 전공 목록 조회 성공")
    void getMajorsByUniversity_success() {
        // given
        Long universityId = 1L;
        University university = University.builder().id(universityId).name("uc11cuc6b8ub300ud559uad50").build();
        Major major1 = Major.builder().id(1L).university(university).name("컴퓨터공학과").build();
        Major major2 = Major.builder().id(2L).university(university).name("수학과").build();
        List<Major> majors = Arrays.asList(major1, major2);

        when(universityRepository.getReferenceById(universityId)).thenReturn(university);
        when(majorRepository.findByUniversity(university)).thenReturn(majors);

        // when
        List<MajorResponse> result = majorService.getMajorsByUniversity(universityId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("컴퓨터공학과");
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("수학과");

        verify(universityRepository, times(1)).getReferenceById(universityId);
        verify(majorRepository, times(1)).findByUniversity(university);
    }

    @Test
    @DisplayName("전공 생성 성공")
    void createMajor_success() {
        // given
        Long universityId = 1L;
        MajorRequest request = new MajorRequest(universityId, "ucef4ud4e8ud130uacf5ud559uacfc");
        University university = University.builder().id(universityId).name("uc11cuc6b8ub300ud559uad50").build();
        Major major = Major.builder().id(1L).university(university).name("ucef4ud4e8ud130uacf5ud559uacfc").build();

        when(universityRepository.getReferenceById(universityId)).thenReturn(university);
        when(majorRepository.existsByUniversityAndName(university, request.name())).thenReturn(false);
        when(majorRepository.save(any(Major.class))).thenReturn(major);

        // when
        MajorResponse result = majorService.createMajor(request);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("ucef4ud4e8ud130uacf5ud559uacfc");

        verify(universityRepository, times(1)).getReferenceById(universityId);
        verify(majorRepository, times(1)).existsByUniversityAndName(university, request.name());
        verify(majorRepository, times(1)).save(any(Major.class));
    }

    @Test
    @DisplayName("전공 생성 실패 - 중복된 이름")
    void createMajor_fail_duplicateName() {
        // given
        Long universityId = 1L;
        MajorRequest request = new MajorRequest(universityId, "ucef4ud4e8ud130uacf5ud559uacfc");
        University university = University.builder().id(universityId).name("uc11cuc6b8ub300ud559uad50").build();

        when(universityRepository.getReferenceById(universityId)).thenReturn(university);
        when(majorRepository.existsByUniversityAndName(university, request.name())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> majorService.createMajor(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 전공 이름입니다");

        verify(universityRepository, times(1)).getReferenceById(universityId);
        verify(majorRepository, times(1)).existsByUniversityAndName(university, request.name());
        verify(majorRepository, never()).save(any(Major.class));
    }

    @Test
    @DisplayName("전공 수정 성공")
    void updateMajor_success() {
        // given
        Long majorId = 1L;
        Long universityId = 1L;
        MajorRequest request = new MajorRequest(universityId, "컴퓨터공학과 (수정)");
        University university = University.builder().id(universityId).name("uc11cuc6b8ub300ud559uad50").build();
        Major major = Major.builder().id(majorId).university(university).name("ucef4ud4e8ud130uacf5ud559uacfc").build();

        when(majorRepository.findById(majorId)).thenReturn(Optional.of(major));
        when(universityRepository.getReferenceById(universityId)).thenReturn(university);
        when(majorRepository.existsByUniversityAndName(university, request.name())).thenReturn(false);

        // when
        MajorResponse result = majorService.updateMajor(majorId, request);

        // then
        assertThat(result.id()).isEqualTo(majorId);
        assertThat(result.name()).isEqualTo("컴퓨터공학과 (수정)");

        verify(majorRepository, times(1)).findById(majorId);
        verify(universityRepository, times(1)).getReferenceById(universityId);
        verify(majorRepository, times(1)).existsByUniversityAndName(university, request.name());
    }

    @Test
    @DisplayName("전공 수정 실패 - 존재하지 않는 전공")
    void updateMajor_fail_notFound() {
        // given
        Long majorId = 999L;
        Long universityId = 1L;
        MajorRequest request = new MajorRequest(universityId, "컴퓨터공학과 (수정)");

        when(majorRepository.findById(majorId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> majorService.updateMajor(majorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 전공이 존재하지 않습니다");

        verify(majorRepository, times(1)).findById(majorId);
        verify(universityRepository, never()).getReferenceById(anyLong());
        verify(majorRepository, never()).existsByUniversityAndName(any(University.class), anyString());
    }

    @Test
    @DisplayName("전공 수정 실패 - 중복된 이름")
    void updateMajor_fail_duplicateName() {
        // given
        Long majorId = 1L;
        Long universityId = 1L;
        MajorRequest request = new MajorRequest(universityId, "uc218ud559uacfc");
        University university = University.builder().id(universityId).name("uc11cuc6b8ub300ud559uad50").build();
        Major major = Major.builder().id(majorId).university(university).name("ucef4ud4e8ud130uacf5ud559uacfc").build();

        when(majorRepository.findById(majorId)).thenReturn(Optional.of(major));
        when(universityRepository.getReferenceById(universityId)).thenReturn(university);
        when(majorRepository.existsByUniversityAndName(university, request.name())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> majorService.updateMajor(majorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 전공 이름입니다");

        verify(majorRepository, times(1)).findById(majorId);
        verify(universityRepository, times(1)).getReferenceById(universityId);
        verify(majorRepository, times(1)).existsByUniversityAndName(university, request.name());
    }

    @Test
    @DisplayName("전공 삭제 성공")
    void deleteMajor_success() {
        // given
        Long majorId = 1L;
        University university = University.builder().id(1L).name("서울대학교").build();
        Major major = Major.builder().id(majorId).university(university).name("ucef4ud4e8ud130uacf5ud559uacfc").build();

        when(majorRepository.findById(majorId)).thenReturn(Optional.of(major));
        doNothing().when(majorRepository).delete(major);

        // when
        majorService.deleteMajor(majorId);

        // then
        verify(majorRepository, times(1)).findById(majorId);
        verify(majorRepository, times(1)).delete(major);
    }

    @Test
    @DisplayName("전공 삭제 실패 - 존재하지 않는 전공")
    void deleteMajor_fail_notFound() {
        // given
        Long majorId = 999L;

        when(majorRepository.findById(majorId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> majorService.deleteMajor(majorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 전공이 존재하지 않습니다");

        verify(majorRepository, times(1)).findById(majorId);
        verify(majorRepository, never()).delete(any(Major.class));
    }
}
