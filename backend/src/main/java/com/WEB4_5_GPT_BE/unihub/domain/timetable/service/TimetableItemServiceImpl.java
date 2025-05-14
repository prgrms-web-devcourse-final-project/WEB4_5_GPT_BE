package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableCourseAddRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item.TimetableItemNormalCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableItemServiceImpl implements TimetableItemService {
    @Override
    public void addCustomItem(Member member, TimetableItemNormalCreateRequest request) {

    }

    @Override
    public void addCourseItem(Member member, TimetableCourseAddRequest request) {

    }

    @Override
    public void bulkRegisterFromEnrollment(Member member) {

    }

    @Override
    public TimetableItemDetailResponse getItemDetail(Member member, Long timetableItemId) {
        return null;
    }

    @Override
    public void updateItem(Member member, Long timetableItemId, TimetableItemUpdateRequest request) {

    }

    @Override
    public void deleteItem(Member member, Long timetableItemId) {

    }
}
