package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SemesterUpdateTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    @DisplayName("학기 업데이트 페이징 정상 동작 테스트")
    void testUpdateAllStudentSemesters() {
        // Given
        List<StudentProfile> studentList = new ArrayList<>();
        studentList.add(mock(StudentProfile.class));

        Page<StudentProfile> studentPage = new PageImpl<>(studentList);
        Page<StudentProfile> emptyPage = new PageImpl<>(Collections.emptyList());

        // mock 설정: findAll 사용 시 처음에는 데이터 있는 페이지, 두 번째는 빈 페이지 반환
        when(studentProfileRepository.findAll(any(Pageable.class)))
                .thenReturn(studentPage)
                .thenReturn(emptyPage);

        // When
        memberService.updateAllStudentSemesters();

        // Then
        // 1. findAll 메소드 호출 확인 (적어도 1번 이상)
        verify(studentProfileRepository, atLeastOnce()).findAll(any(Pageable.class));

        // 2. 기본 기능만 확인 (메소드가 오류 없이 완료되는지)
        // 테스트가 여기까지 오면 성공
    }
}
