package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.TimetableItemType;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseScheduleDto;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableBulkRegisterRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableCourseAddRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemNormalCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItem;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItemSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.DuplicateCourseItemException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableItemNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableUnauthorizedException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableItemRepository;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableItemServiceImpl implements TimetableItemService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;
    private final CourseRepository courseRepository;

    /**
     * 시간표에 사용자 정의 항목 추가
     */
    @Override
    @Transactional
    public void createNormalItem(Member member, TimetableItemNormalCreateRequest request) {
        // 시간표 조회
        Timetable timetable = timetableRepository.findById(request.timetableId())
                .orElseThrow(TimetableNotFoundException::new);

        // 시간표 소유자 확인
        if (!timetable.getMember().getId().equals(member.getId())) {
            throw new TimetableUnauthorizedException();
        }

        // 시간표 항목 생성
        TimetableItem timetableItem = TimetableItem.builder()
                .timetable(timetable)
                .type(TimetableItemType.NORMAL)
                .title(request.title())
                .professorName(request.professorName())
                .color(request.color())
                .location(request.location())
                .memo(request.memo())
                .build();

        // 시간표 항목 저장
        TimetableItem savedItem = timetableItemRepository.save(timetableItem);

        // 스케줄 정보 추가
        for (TimetableItemNormalCreateRequest.ScheduleRequest scheduleRequest : request.schedule()) {
            TimetableItemSchedule schedule = TimetableItemSchedule.builder()
                    .timetableItem(savedItem)
                    .day(scheduleRequest.day())
                    .startTime(LocalTime.parse(scheduleRequest.startTime(), TIME_FORMATTER))
                    .endTime(LocalTime.parse(scheduleRequest.endTime(), TIME_FORMATTER))
                    .build();

            // 연관 관계를 통해 자동으로 저장될 수 있도록 추가
            savedItem.getSchedules().add(schedule);
        }
    }

    /**
     * 시간표에 강의 추가
     */
    @Override
    @Transactional
    public void addCourseItem(Member member, TimetableCourseAddRequest request) {
        // 시간표 조회
        Timetable timetable = timetableRepository.findById(request.timetableId())
                .orElseThrow(TimetableNotFoundException::new);

        // 시간표 소유자 확인
        if (!timetable.getMember().getId().equals(member.getId())) {
            throw new TimetableUnauthorizedException();
        }

        // 강의 조회
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(CourseNotFoundException::new);

        // 이미 등록된 강의인지 확인
        Optional<TimetableItem> existingItem = timetableItemRepository.findByTimetableIdAndCourse(timetable.getId(), course);
        if (existingItem.isPresent()) {
            throw new DuplicateCourseItemException();
        }

        // 시간표 항목 생성
        TimetableItem timetableItem = TimetableItem.builder()
                .timetable(timetable)
                .course(course)
                .type(TimetableItemType.COURSE)
                .title(course.getTitle())
                .professorName(course.getProfessor().getName())
                .color(request.color())
                .location(course.getLocation())
                .memo(request.memo())
                .build();

        // 시간표 항목 저장
        TimetableItem savedItem = timetableItemRepository.save(timetableItem);

        // 강의 스케줄 정보 추가
        for (CourseSchedule courseSchedule : course.getSchedules()) {
            TimetableItemSchedule schedule = TimetableItemSchedule.builder()
                    .timetableItem(savedItem)
                    .day(courseSchedule.getDay())
                    .startTime(courseSchedule.getStartTime())
                    .endTime(courseSchedule.getEndTime())
                    .build();

            savedItem.getSchedules().add(schedule);
        }
    }

    /**
     * 수강 중인 강의 전체 반영
     */
    @Override
    @Transactional
    public void bulkRegisterFromEnrollment(Member member, TimetableBulkRegisterRequest request) {
        // 요청에서 시간표 ID와 강의 ID 목록 가져오기
        Long timetableId = request.timetableId();
        List<Long> courseIds = request.courseIds();

        if (courseIds == null || courseIds.isEmpty()) {
            return; // 등록할 강의가 없으면 처리할 것 없음
        }

        // 시간표 조회 (권한 확인)
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(TimetableNotFoundException::new);

        // 권한 확인 (본인 시간표인지)
        if (!timetable.getMember().getId().equals(member.getId())) {
            throw new TimetableUnauthorizedException();
        }

        // 현재 시간표에 등록된 강의 항목 조회
        List<TimetableItem> existingItems = timetableItemRepository.findWithSchedulesByTimetableId(timetable.getId());
        List<Course> existingCourses = existingItems.stream()
                .map(TimetableItem::getCourse)
                .filter(Objects::nonNull)
                .toList();

        // 요청에 포함된 각 강의 ID에 대해 강의를 조회하고 시간표에 추가
        for (Long courseId : courseIds) {
            // 강의 조회
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(CourseNotFoundException::new);

            // 이미 등록된 강의라면 건너뛔
            if (existingCourses.contains(course)) {
                continue;
            }

            // 항목 생성 및 저장
            TimetableItem item = TimetableItem.builder()
                    .timetable(timetable)
                    .course(course)
                    .type(TimetableItemType.COURSE)
                    .title(course.getTitle())
                    .professorName(course.getProfessor().getName())
                    .color(generateRandomColor()) // 랜덤 색상 생성
                    .location(course.getLocation())
                    .build();

            TimetableItem savedItem = timetableItemRepository.save(item);

            // 강의 스케줄 정보 추가
            for (CourseSchedule courseSchedule : course.getSchedules()) {
                TimetableItemSchedule schedule = TimetableItemSchedule.builder()
                        .timetableItem(savedItem)
                        .day(courseSchedule.getDay())
                        .startTime(courseSchedule.getStartTime())
                        .endTime(courseSchedule.getEndTime())
                        .build();

                savedItem.getSchedules().add(schedule);
            }
        }
    }

    /**
     * 시간표 항목 단건 조회
     */
    @Override
    public TimetableItemDetailResponse getItemDetail(Member member, Long timetableItemId) {
        // 시간표 항목 조회
        TimetableItem timetableItem = timetableItemRepository.findWithSchedulesById(timetableItemId)
                .orElseThrow(TimetableItemNotFoundException::new);

        // 시간표 소유자 확인
        if (!timetableItem.getTimetable().getMember().getId().equals(member.getId())) {
            throw new TimetableUnauthorizedException();
        }

        Timetable timetable = timetableItem.getTimetable();

        // 스케줄 정보 변환
        List<TimetableItemDetailResponse.ScheduleDto> schedules = timetableItem.getSchedules().stream()
                .map(schedule -> new TimetableItemDetailResponse.ScheduleDto(
                        schedule.getDay().toString(),
                        schedule.getStartTime().format(TIME_FORMATTER),
                        schedule.getEndTime().format(TIME_FORMATTER)
                ))
                .collect(Collectors.toList());

        // 응답 DTO 반환
        return new TimetableItemDetailResponse(
                timetable.getYear(),
                timetable.getSemester(),
                timetableItem.getTitle(),
                timetableItem.getProfessorName(),
                timetableItem.getLocation(),
                timetableItem.getColor(),
                timetableItem.getMemo(),
                schedules
        );
    }

    /**
     * 시간표 항목 수정
     */
    @Override
    @Transactional
    public void updateItem(Member member, Long timetableItemId, TimetableItemUpdateRequest request) {
        // 시간표 항목 조회
        TimetableItem timetableItem = timetableItemRepository.findWithSchedulesById(timetableItemId)
                .orElseThrow(TimetableItemNotFoundException::new);

        // 시간표 소유자 확인
        if (!timetableItem.getTimetable().getMember().getId().equals(member.getId())) {
            throw new TimetableUnauthorizedException();
        }

        // 기본 정보 업데이트
        timetableItem.setTitle(request.title());
        timetableItem.setProfessorName(request.professorName());
        timetableItem.setColor(request.color());
        timetableItem.setLocation(request.location());
        timetableItem.setMemo(request.memo());

        // 강의인 경우 수정가능한 값만 업데이트
        if (timetableItem.getType() == TimetableItemType.COURSE) {
            // 강의의 경우 시간표 스케줄 정보는 강의 정보에 맞춰 개인 설정 불가
            return;
        }

        // 기존 스케줄 삭제
        timetableItem.getSchedules().clear();

        // 새 스케줄 추가
        for (CourseScheduleDto scheduleDto : request.schedule()) {
            DayOfWeek day = scheduleDto.day();
            LocalTime startTime = LocalTime.parse(scheduleDto.startTime(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(scheduleDto.endTime(), TIME_FORMATTER);

            TimetableItemSchedule schedule = TimetableItemSchedule.builder()
                    .timetableItem(timetableItem)
                    .day(day)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();

            timetableItem.getSchedules().add(schedule);
        }
    }

    /**
     * 시간표 항목 삭제
     */
    @Override
    @Transactional
    public void deleteItem(Member member, Long timetableItemId) {
        // 해당 항목을 소유한 멤버인지 확인
        boolean isOwnedByMember = timetableItemRepository.existsByIdAndMemberId(timetableItemId, member.getId());

        if (!isOwnedByMember) {
            throw new TimetableUnauthorizedException();
        }

        // 항목 삭제 (연결된 스케줄 기록도 케스케이드로 함께 삭제됨)
        timetableItemRepository.deleteById(timetableItemId);
    }

    /**
     * 랜덤 색상 생성 함수
     */
    private String generateRandomColor() {
        // 시간표용 다양한 색상 목록
        List<String> colors = List.of(
                "#4285F4", "#EA4335", "#FBBC05", "#34A853",
                "#3498DB", "#9B59B6", "#E74C3C", "#E67E22", "#F1C40F",
                "#1ABC9C", "#2ECC71", "#FF6B6B", "#747D8C", "#5F27CD"
        );

        // 랜덤 색상 선택
        int randomIndex = (int) (Math.random() * colors.size());
        return colors.get(randomIndex);
    }
}
