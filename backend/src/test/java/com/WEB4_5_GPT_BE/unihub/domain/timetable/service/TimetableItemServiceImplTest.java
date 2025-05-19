package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.TimetableItemType;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableCourseAddRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemNormalCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItem;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItemSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.DuplicateCourseItemException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableUnauthorizedException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableItemRepository;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimetableItemServiceImplTest {

    @Mock
    private TimetableRepository timetableRepository;

    @Mock
    private TimetableItemRepository timetableItemRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private TimetableItemServiceImpl timetableItemService;

    // 테스트 도우미 메서드들
    private Student createStudent(Long id, String name, String email) {
        return Student.builder()
                .id(id)
                .name(name)
                .email(email)
                .major(Major.builder().id(1L).name("컴퓨터공학과").build())
                .university(University.builder().id(1L).name("A대학교").build())
                .build();
    }

    private Professor createProfessor() {
        return Professor.builder()
                .id(3L)
                .name("박교수")
                .build();
    }

    private Course createCourse(Long id, Professor professor) {
        Course course = Course.builder()
                .id(id)
                .title("알고리즘")
                .location("공학관 401호")
                .professor(professor)
                .semester(1)
                .capacity(30)
                .enrolled(10)
                .credit(3)
                .grade(3)
                .major(Major.builder().id(1L).name("컴퓨터공학과").build())
                .build();

        // BaseTimeEntity의 createdAt 설정을 위한 리플렉션 사용 대신
        // 테스트 목적으로 직접 필드에 접근할 수 있게 만든 코드를 추가해야 하지만,
        // 테스트에서는 mock을 사용하므로 필요한 시점에 when 구문으로 처리
        return course;
    }

    private TimetableItemNormalCreateRequest createNormalItemRequest(Long timetableId) {
        List<TimetableItemNormalCreateRequest.ScheduleRequest> schedules = new ArrayList<>();
        schedules.add(new TimetableItemNormalCreateRequest.ScheduleRequest(
                DayOfWeek.MON, "09:00", "10:30"
        ));

        return new TimetableItemNormalCreateRequest(
                timetableId,
                "알고리즘 스터디",
                "홍길동",
                "#4285F4",
                "도서관 스터디룸",
                "알고리즘 문제 풀이 스터디",
                schedules
        );
    }

    private TimetableCourseAddRequest createCourseItemRequest(Long timetableId, Long courseId) {
        return new TimetableCourseAddRequest(
                timetableId,
                courseId,
                "#EA4335",
                "중간고사 5/10"
        );
    }

    @Test
    @DisplayName("일반 항목 추가 - 정상 케이스")
    void givenValidRequest_whenAddCustomItem_thenSavesTimetableItem() {
        // given
        Student student = createStudent(1L, "김학생", "student@univ.ac.kr");
        Long timetableId = 1L;
        TimetableItemNormalCreateRequest request = createNormalItemRequest(timetableId);

        Timetable timetable = Timetable.builder()
                .id(timetableId)
                .member(student)
                .year(2025)
                .semester(1)
                .build();

        when(timetableRepository.findById(timetableId)).thenReturn(Optional.of(timetable));
        when(timetableItemRepository.save(any(TimetableItem.class)))
                .thenAnswer(invocation -> {
                    TimetableItem item = invocation.getArgument(0);
                    // save 메서드 호출 시 ID 부여 시뮬레이션
                    // 실제 JPA는 저장 시 ID를 부여함
                    return TimetableItem.builder()
                            .id(10L)
                            .timetable(item.getTimetable())
                            .type(item.getType())
                            .title(item.getTitle())
                            .professorName(item.getProfessorName())
                            .color(item.getColor())
                            .location(item.getLocation())
                            .memo(item.getMemo())
                            .build();
                });

        // when
        timetableItemService.createNormalItem(student, request);

        // then
        verify(timetableRepository).findById(timetableId);

        ArgumentCaptor<TimetableItem> itemCaptor = ArgumentCaptor.forClass(TimetableItem.class);
        verify(timetableItemRepository).save(itemCaptor.capture());

        TimetableItem capturedItem = itemCaptor.getValue();
        assertThat(capturedItem.getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(capturedItem.getType()).isEqualTo(TimetableItemType.NORMAL);
        assertThat(capturedItem.getColor()).isEqualTo("#4285F4");
    }

    @Test
    @DisplayName("시간표가 존재하지 않을 때 일반 항목 추가 - 예외 발생")
    void givenNonExistentTimetable_whenAddCustomItem_thenThrowsException() {
        // given
        Student student = createStudent(1L, "김학생", "student@univ.ac.kr");
        Long timetableId = 999L; // 존재하지 않는 ID
        TimetableItemNormalCreateRequest request = createNormalItemRequest(timetableId);

        when(timetableRepository.findById(timetableId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> timetableItemService.createNormalItem(student, request))
                .isInstanceOf(TimetableNotFoundException.class);

        verify(timetableItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 사용자의 시간표에 항목 추가 - 예외 발생")
    void givenOtherUsersTimetable_whenAddCustomItem_thenThrowsException() {
        // given
        Student currentUser = createStudent(1L, "김학생", "student@univ.ac.kr");
        Student otherUser = createStudent(2L, "이학생", "other@univ.ac.kr");
        Long timetableId = 1L;
        TimetableItemNormalCreateRequest request = createNormalItemRequest(timetableId);

        Timetable timetable = Timetable.builder()
                .id(timetableId)
                .member(otherUser) // 다른 사용자의 시간표
                .year(2025)
                .semester(1)
                .build();

        when(timetableRepository.findById(timetableId)).thenReturn(Optional.of(timetable));

        // when/then
        assertThatThrownBy(() -> timetableItemService.createNormalItem(currentUser, request))
                .isInstanceOf(TimetableUnauthorizedException.class);

        verify(timetableItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("강의 항목 추가 - 정상 케이스")
    void givenValidRequest_whenAddCourseItem_thenSavesTimetableItem() {
        // given
        Student student = createStudent(1L, "김학생", "student@univ.ac.kr");
        Professor professor = createProfessor();
        Long timetableId = 1L;
        Long courseId = 5L;

        Timetable timetable = Timetable.builder()
                .id(timetableId)
                .member(student)
                .year(2025)
                .semester(1)
                .build();

        Course course = createCourse(courseId, professor);

        TimetableCourseAddRequest request = createCourseItemRequest(timetableId, courseId);

        when(timetableRepository.findById(timetableId)).thenReturn(Optional.of(timetable));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(timetableItemRepository.findByTimetableIdAndCourse(timetableId, course))
                .thenReturn(Optional.empty());
        when(timetableItemRepository.save(any(TimetableItem.class)))
                .thenAnswer(invocation -> {
                    TimetableItem item = invocation.getArgument(0);
                    return TimetableItem.builder()
                            .id(20L)
                            .timetable(item.getTimetable())
                            .course(item.getCourse())
                            .type(item.getType())
                            .title(item.getTitle())
                            .professorName(item.getProfessorName())
                            .color(item.getColor())
                            .location(item.getLocation())
                            .memo(item.getMemo())
                            .build();
                });

        // when
        timetableItemService.addCourseItem(student, request);

        // then
        verify(timetableRepository).findById(timetableId);
        verify(courseRepository).findById(courseId);
        verify(timetableItemRepository).findByTimetableIdAndCourse(timetableId, course);

        ArgumentCaptor<TimetableItem> itemCaptor = ArgumentCaptor.forClass(TimetableItem.class);
        verify(timetableItemRepository).save(itemCaptor.capture());

        TimetableItem capturedItem = itemCaptor.getValue();
        assertThat(capturedItem.getType()).isEqualTo(TimetableItemType.COURSE);
        assertThat(capturedItem.getCourse()).isEqualTo(course);
        assertThat(capturedItem.getColor()).isEqualTo("#EA4335");
        assertThat(capturedItem.getMemo()).isEqualTo("중간고사 5/10");
    }

    @Test
    @DisplayName("이미 등록된 강의 추가 시도 - 예외 발생")
    void givenDuplicateCourse_whenAddCourseItem_thenThrowsException() {
        // given
        Student student = createStudent(1L, "김학생", "student@univ.ac.kr");
        Professor professor = createProfessor();
        Long timetableId = 1L;
        Long courseId = 5L;

        Timetable timetable = Timetable.builder()
                .id(timetableId)
                .member(student)
                .year(2025)
                .semester(1)
                .build();

        Course course = createCourse(courseId, professor);
        TimetableCourseAddRequest request = createCourseItemRequest(timetableId, courseId);

        // 이미 등록된 강의가 있음을 시뮬레이션
        TimetableItem existingItem = TimetableItem.builder()
                .id(20L)
                .timetable(timetable)
                .course(course)
                .type(TimetableItemType.COURSE)
                .title("알고리즘")
                .professorName("박교수")
                .color("#EA4335")
                .build();

        when(timetableRepository.findById(timetableId)).thenReturn(Optional.of(timetable));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(timetableItemRepository.findByTimetableIdAndCourse(timetableId, course))
                .thenReturn(Optional.of(existingItem));

        // when/then
        assertThatThrownBy(() -> timetableItemService.addCourseItem(student, request))
                .isInstanceOf(DuplicateCourseItemException.class);

        verify(timetableItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("항목 단건 조회 - 정상 케이스")
    void givenValidItemId_whenGetItemDetail_thenReturnsDetail() {
        // given
        Student student = createStudent(1L, "김학생", "student@univ.ac.kr");
        Long timetableId = 1L;
        Long itemId = 10L;

        Timetable timetable = Timetable.builder()
                .id(timetableId)
                .member(student)
                .year(2025)
                .semester(1)
                .build();

        TimetableItem item = mock(TimetableItem.class);
        when(item.getTimetable()).thenReturn(timetable);
        when(item.getTitle()).thenReturn("알고리즘 스터디");
        when(item.getColor()).thenReturn("#4285F4");

        List<TimetableItemSchedule> schedules = new ArrayList<>();
        schedules.add(TimetableItemSchedule.builder()
                .id(100L)
                .timetableItem(item)
                .day(DayOfWeek.MON)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build());

        // 항목 스케줄 설정
        when(item.getSchedules()).thenReturn(schedules);

        when(timetableItemRepository.findWithSchedulesById(itemId))
                .thenReturn(Optional.of(item));

        // when
        TimetableItemDetailResponse response = timetableItemService.getItemDetail(student, itemId);

        // then
        assertThat(response.title()).isEqualTo("알고리즘 스터디");
        assertThat(response.year()).isEqualTo(2025);
        assertThat(response.semester()).isEqualTo(1);
        assertThat(response.color()).isEqualTo("#4285F4");
        assertThat(response.schedule()).hasSize(1);
        assertThat(response.schedule().get(0).day()).isEqualTo("MON");
        assertThat(response.schedule().get(0).startTime()).isEqualTo("09:00");
        assertThat(response.schedule().get(0).endTime()).isEqualTo("10:30");
    }


    @Test
    @DisplayName("항목 삭제 - 정상 케이스")
    void givenItemId_whenDeleteItem_thenDeletesItem() {
        // given
        Student student = createStudent(1L, "김학생", "student@univ.ac.kr");
        Long itemId = 10L;

        when(timetableItemRepository.existsByIdAndMemberId(itemId, student.getId())).thenReturn(true);

        // when
        timetableItemService.deleteItem(student, itemId);

        // then
        verify(timetableItemRepository).existsByIdAndMemberId(itemId, student.getId());
        verify(timetableItemRepository).deleteById(itemId);
    }
}
