package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableSemesterResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableShareLinkResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableSharedViewResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableAlreadyExistsException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;

    @Override
    @Transactional
    public void createTimetable(Member member, TimetableCreateRequest request) {
        validateDuplicateTimetable(member, request);

        Timetable timetable = buildTimetable(member, request);

        timetableRepository.save(timetable);
    }

    private void validateDuplicateTimetable(Member member, TimetableCreateRequest request) {
        boolean exists = timetableRepository.existsByMemberIdAndYearAndSemester(
                member.getId(), request.year(), request.semester()
        );
        if (exists) {
            throw new TimetableAlreadyExistsException();
        }
    }

    private Timetable buildTimetable(Member member, TimetableCreateRequest request) {
        return Timetable.builder()
                .member(member)
                .year(request.year())
                .semester(request.semester())
                .build();
    }

    @Override
    public TimetableDetailResponse getMyTimetable(Member member, int year, int semester) {
        Timetable timetable = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), year, semester)
                .orElseThrow(TimetableNotFoundException::new);

        return TimetableDetailResponse.of(timetable);
    }

    @Override
    public List<TimetableSemesterResponse> getRegisteredSemesters(Member member) {
        return null;
    }

    @Override
    public TimetableShareLinkResponse createShareLink(Member member, TimetableShareLinkRequest request) {
        return null;
    }

    @Override
    public TimetableSharedViewResponse getSharedTimetable(String shareKey) {
        return null;
    }
}
