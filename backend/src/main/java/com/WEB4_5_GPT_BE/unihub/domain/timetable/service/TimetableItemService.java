package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableCourseAddRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemNormalCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemUpdateRequest;

public interface TimetableItemService {

    // 시간표에 직접 등록
    void addCustomItem(Member member, TimetableItemNormalCreateRequest request);

    // 시간표에 강의 등록
    void addCourseItem(Member member, TimetableCourseAddRequest request);

    // 수강 중인 강의 전체 반영
    void bulkRegisterFromEnrollment(Member member);

    // 시간표 항목 단건 조회
    TimetableItemDetailResponse getItemDetail(Member member, Long timetableItemId);

    // 시간표 항목 수정
    void updateItem(Member member, Long timetableItemId, TimetableItemUpdateRequest request);

    // 시간표 항목 삭제
    void deleteItem(Member member, Long timetableItemId);
}
