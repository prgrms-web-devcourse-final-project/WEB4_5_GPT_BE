package com.WEB4_5_GPT_BE.unihub.domain.university.service;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.UniversityRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
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
public class UniversityServiceTest {

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private UniversityService universityService;

    @Test
    @DisplayName("모든 대학 조회 성공")
    void getAllUniversities_success() {
        // given
        University university1 = University.builder().id(1L).name("서울대학교").build();
        University university2 = University.builder().id(2L).name("연세대학교").build();
        List<University> universities = Arrays.asList(university1, university2);

        when(universityRepository.findAll()).thenReturn(universities);

        // when
        List<UniversityResponse> result = universityService.getAllUniversities();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("서울대학교");
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("연세대학교");

        verify(universityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("대학 단건 조회 성공")
    void getUniversity_success() {
        // given
        Long universityId = 1L;
        University university = University.builder().id(universityId).name("서울대학교").build();

        when(universityRepository.findById(universityId)).thenReturn(Optional.of(university));

        // when
        UniversityResponse result = universityService.getUniversity(universityId);

        // then
        assertThat(result.id()).isEqualTo(universityId);
        assertThat(result.name()).isEqualTo("서울대학교");

        verify(universityRepository, times(1)).findById(universityId);
    }

    @Test
    @DisplayName("대학 단건 조회 실패 - 존재하지 않는 대학")
    void getUniversity_fail_notFound() {
        // given
        Long universityId = 999L;

        when(universityRepository.findById(universityId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> universityService.getUniversity(universityId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 대학이 존재하지 않습니다");

        verify(universityRepository, times(1)).findById(universityId);
    }

    @Test
    @DisplayName("대학 생성 성공")
    void createUniversity_success() {
        // given
        UniversityRequest request = new UniversityRequest("서울대학교");
        University university = University.builder().id(1L).name("서울대학교").build();

        when(universityRepository.existsByName(request.name())).thenReturn(false);
        when(universityRepository.save(any(University.class))).thenReturn(university);

        // when
        UniversityResponse result = universityService.createUniversity(request);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("서울대학교");

        verify(universityRepository, times(1)).existsByName(request.name());
        verify(universityRepository, times(1)).save(any(University.class));
    }

    @Test
    @DisplayName("대학 생성 실패 - 중복된 이름")
    void createUniversity_fail_duplicateName() {
        // given
        UniversityRequest request = new UniversityRequest("서울대학교");

        when(universityRepository.existsByName(request.name())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> universityService.createUniversity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 대학입니다");

        verify(universityRepository, times(1)).existsByName(request.name());
        verify(universityRepository, never()).save(any(University.class));
    }

    @Test
    @DisplayName("대학 수정 성공")
    void updateUniversity_success() {
        // given
        Long universityId = 1L;
        UniversityRequest request = new UniversityRequest("서울대학교 (수정)");
        University university = University.builder().id(universityId).name("서울대학교").build();

        when(universityRepository.findById(universityId)).thenReturn(Optional.of(university));
        when(universityRepository.existsByName(request.name())).thenReturn(false);

        // when
        UniversityResponse result = universityService.updateUniversity(universityId, request);

        // then
        assertThat(result.id()).isEqualTo(universityId);
        assertThat(result.name()).isEqualTo("서울대학교 (수정)");

        verify(universityRepository, times(1)).findById(universityId);
        verify(universityRepository, times(1)).existsByName(request.name());
    }

    @Test
    @DisplayName("대학 수정 실패 - 존재하지 않는 대학")
    void updateUniversity_fail_notFound() {
        // given
        Long universityId = 999L;
        UniversityRequest request = new UniversityRequest("서울대학교 (수정)");

        when(universityRepository.findById(universityId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> universityService.updateUniversity(universityId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 대학이 존재하지 않습니다");

        verify(universityRepository, times(1)).findById(universityId);
        verify(universityRepository, never()).existsByName(anyString());
    }

    @Test
    @DisplayName("대학 수정 실패 - 중복된 이름")
    void updateUniversity_fail_duplicateName() {
        // given
        Long universityId = 1L;
        UniversityRequest request = new UniversityRequest("연세대학교");
        University university = University.builder().id(universityId).name("서울대학교").build();

        when(universityRepository.findById(universityId)).thenReturn(Optional.of(university));
        when(universityRepository.existsByName(request.name())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> universityService.updateUniversity(universityId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 대학 이름입니다");

        verify(universityRepository, times(1)).findById(universityId);
        verify(universityRepository, times(1)).existsByName(request.name());
    }

    @Test
    @DisplayName("대학 삭제 성공")
    void deleteUniversity_success() {
        // given
        Long universityId = 1L;
        University university = University.builder().id(universityId).name("서울대학교").build();

        when(universityRepository.findById(universityId)).thenReturn(Optional.of(university));
        doNothing().when(universityRepository).delete(university);

        // when
        universityService.deleteUniversity(universityId);

        // then
        verify(universityRepository, times(1)).findById(universityId);
        verify(universityRepository, times(1)).delete(university);
    }

    @Test
    @DisplayName("대학 삭제 실패 - 존재하지 않는 대학")
    void deleteUniversity_fail_notFound() {
        // given
        Long universityId = 999L;

        when(universityRepository.findById(universityId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> universityService.deleteUniversity(universityId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 대학이 존재하지 않습니다");

        verify(universityRepository, times(1)).findById(universityId);
        verify(universityRepository, never()).delete(any(University.class));
    }
}
