package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableCourseAddRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemNormalCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemUpdateRequest;

public interface TimetableItemService {

    // 시간표에 직접 등록
    void addCustomItem(Long memberId, TimetableItemNormalCreateRequest request);

    // 시간표에 강의 등록
    void addCourseItem(Long memberId, TimetableCourseAddRequest request);

    // 수강 중인 강의 전체 반영
    void bulkRegisterFromEnrollment(Long memberId);

    // 시간표 항목 단건 조회
    TimetableItemDetailResponse getItemDetail(Long memberId, Long timetableItemId);

    // 시간표 항목 수정
    void updateItem(Long memberId, Long timetableItemId, TimetableItemUpdateRequest request);

    // 시간표 항목 삭제
    void deleteItem(Long memberId, Long timetableItemId);
}
