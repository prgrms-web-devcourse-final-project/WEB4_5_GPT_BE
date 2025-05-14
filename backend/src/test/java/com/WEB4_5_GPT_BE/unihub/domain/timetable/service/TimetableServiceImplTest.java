package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableAlreadyExistsException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimetableServiceImplTest {

    private TimetableRepository timetableRepository;
    private TimetableServiceImpl timetableService;

    @BeforeEach
    void setUp() {
        timetableRepository = mock(TimetableRepository.class);
        timetableService = new TimetableServiceImpl(timetableRepository);
    }

    @Test
    @DisplayName("시간표가 중복되면 예외를 발생시킨다")
    void givenDuplicateTimetable_whenCreateTimetable_thenThrowException() {
        // given
        Professor member = Professor.builder()
                .id(1L)
                .name("김교수")
                .email("professor@auni.ac.kr")
                .major(Major.builder().id(1L).name("소프트웨어").build())
                .university(University.builder().id(1L).name("A대학교").build())
                .build();

        TimetableCreateRequest request = new TimetableCreateRequest(2025, 1);

        when(timetableRepository.existsByMemberIdAndYearAndSemester(1L, 2025, 1)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> timetableService.createTimetable(member, request))
                .isInstanceOf(TimetableAlreadyExistsException.class)
                .hasMessage("해당 학기 시간표는 이미 존재합니다.");

        verify(timetableRepository, never()).save(any(Timetable.class));
    }

    @Test
    @DisplayName("시간표가 존재하지 않으면 새 시간표를 저장한다")
    void givenValidRequest_whenCreateTimetable_thenSavesTimetable() {
        // given
        Professor member = Professor.builder()
                .id(1L)
                .name("김교수")
                .email("professor@auni.ac.kr")
                .major(Major.builder().id(1L).name("소프트웨어").build())
                .university(University.builder().id(1L).name("A대학교").build())
                .build();

        TimetableCreateRequest request = new TimetableCreateRequest(2025, 1);
        when(timetableRepository.existsByMemberIdAndYearAndSemester(1L, 2025, 1)).thenReturn(false);

        // when
        timetableService.createTimetable(member, request);

        // then
        ArgumentCaptor<Timetable> captor = ArgumentCaptor.forClass(Timetable.class);
        verify(timetableRepository).save(captor.capture());

        Timetable saved = captor.getValue();
        assertThat(saved.getMember()).isEqualTo(member);
        assertThat(saved.getYear()).isEqualTo(2025);
        assertThat(saved.getSemester()).isEqualTo(1);
    }
}
