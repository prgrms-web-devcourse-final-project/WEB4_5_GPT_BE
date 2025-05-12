package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.scheduler.SemesterUpdateScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SemesterUpdateTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberService memberServiceMock;

    @Test
    @DisplayName("학기 업데이트 페이징 정상 동작 테스트")
    void testUpdateAllStudentSemesters() {
        // Given
        List<Student> studentList = new ArrayList<>();
        studentList.add(mock(Student.class));

        Page<Student> studentPage = new PageImpl<>(studentList);
        Page<Student> emptyPage = new PageImpl<>(Collections.emptyList());

        // mock 설정: findAll 사용 시 처음에는 데이터 있는 페이지, 두 번째는 빈 페이지 반환
        when(studentRepository.findAll(any(Pageable.class)))
                .thenReturn(studentPage)
                .thenReturn(emptyPage);

        // When
        memberService.updateAllStudentSemesters();

        // Then
        // 1. findAll 메소드 호출 확인 (적어도 1번 이상)
        verify(studentRepository, atLeastOnce()).findAll(any(Pageable.class));

        // 2. 기본 기능만 확인 (메소드가 오류 없이 완료되는지)
        // 테스트가 여기까지 오면 성공
    }

    @Test
    @DisplayName("봄학기 스케줄러 테스트 - 3월 1일 새학기")
    void testSchedulerForSpring() {
        // Given
        // 3월 1일 날짜로 가정
        SemesterUpdateScheduler scheduler = new SemesterUpdateScheduler(memberServiceMock);

        // 학기 업데이트 메소드 호출 확인을 위한 Mock 설정
        doNothing().when(memberServiceMock).updateAllStudentSemesters();

        // When
        scheduler.updateSemesterForSpring();

        // Then
        verify(memberServiceMock, times(1)).updateAllStudentSemesters();
    }

    @Test
    @DisplayName("가을학기 스케줄러 테스트 - 9월 1일 새학기")
    void testSchedulerForFall() {
        // Given
        // 9월 1일 날짜로 가정
        SemesterUpdateScheduler scheduler = new SemesterUpdateScheduler(memberServiceMock);

        // 학기 업데이트 메소드 호출 확인을 위한 Mock 설정
        doNothing().when(memberServiceMock).updateAllStudentSemesters();

        // When
        scheduler.updateSemesterForFall();

        // Then
        verify(memberServiceMock, times(1)).updateAllStudentSemesters();
    }
}
