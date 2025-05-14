package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableSemesterResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableShareLinkResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableSharedViewResponse;

import java.util.List;

public interface TimetableService {

    // 시간표 생성
    void createTimetable(Long memberId, TimetableCreateRequest request);

    // 내 시간표 조회
    TimetableDetailResponse getMyTimetable(Long memberId);

    // 등록된 시간표 학기 목록 조회
    List<TimetableSemesterResponse> getRegisteredSemesters(Long memberId);

    // 공유 링크 생성
    TimetableShareLinkResponse createShareLink(Long memberId, TimetableShareLinkRequest request);

    // 공유된 시간표 조회
    TimetableSharedViewResponse getSharedTimetable(String shareKey);
}