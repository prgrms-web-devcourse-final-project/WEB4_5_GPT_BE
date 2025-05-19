package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Visibility;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableShareLinkResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableSharedViewResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share.TimetablePrivateException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share.TimetableShareDeniedException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share.TimetableShareLinkInvalidException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableAlreadyExistsException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableItemRepository;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;
    private final StringRedisTemplate redisTemplate;
    private static final Duration SHARE_TTL        = Duration.ofDays(7);
    private static final int      SHARE_KEY_LENGTH = 8;
    private static final String TIMETABLE_ID  = "timetableId";
    private static final String TIMETABLE_VIS = "visibility";

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
    public TimetableShareLinkResponse createShareLink(Member member, TimetableShareLinkRequest request, String clientBaseUrl) {

        Timetable timetable = validateOwnership(member, request.timetableId());

        // 공유 키 생성 & Redis 저장
        String shareKey = generateShareKey();
        saveShareLinkInRedis(shareKey, timetable.getId(), request.visibility());

        // 응답 변환
        return toResponse(clientBaseUrl, shareKey);
    }

    private Timetable validateOwnership(Member m, Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(TimetableNotFoundException::new);
        if (!timetable.getMember().getId().equals(m.getId())) {
            throw new TimetableShareDeniedException();
        }
        return timetable;
    }

    private void saveShareLinkInRedis(String shareKey, Long timetableId, Visibility visibility) {
        redisTemplate.opsForHash().put(shareKey, TIMETABLE_ID,  timetableId.toString());
        redisTemplate.opsForHash().put(shareKey, TIMETABLE_VIS, visibility.name());
        redisTemplate.expire(shareKey, SHARE_TTL);
    }

    /** 예: “8f94de12” 처럼 하이픈 제거 & 8자리 */
    private String generateShareKey() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, SHARE_KEY_LENGTH);
    }

    private TimetableShareLinkResponse toResponse(String baseUrl, String key) {
        String url = String.format("%s/timetable/share/%s", baseUrl, key);
        LocalDateTime expiresAt = LocalDateTime.now().plus(SHARE_TTL);
        return new TimetableShareLinkResponse(url, expiresAt);
    }

    @Override
    public TimetableSharedViewResponse getSharedTimetable(String shareKey, Member currentUser) {
        Map<Object,Object> entries = fetchAndValidateRedisEntry(shareKey);
        Long timetableId = parseTimetableId(entries);
        Timetable timetable = loadTimetableWithItems(timetableId);
        checkVisibilityWithOwner(entries, timetable, currentUser);
        return toSharedViewResponse(timetable);
    }

    private Map<Object,Object> fetchAndValidateRedisEntry(String shareKey) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(shareKey))) {
            throw new TimetableShareLinkInvalidException();
        }
        Map<Object,Object> entries = redisTemplate.opsForHash().entries(shareKey);
        if (entries.get(TIMETABLE_ID) == null || entries.get(TIMETABLE_VIS) == null) {
            throw new TimetableShareLinkInvalidException();
        }
        return entries;
    }

    private Long parseTimetableId(Map<Object,Object> entries) {
        try {
            return Long.parseLong((String) entries.get(TIMETABLE_ID));
        } catch (Exception e) {
            throw new TimetableShareLinkInvalidException();
        }
    }

    private void checkVisibilityWithOwner(Map<Object,Object> entries, Timetable timetable, Member currentUser) {
        Visibility vis = Visibility.valueOf((String) entries.get(TIMETABLE_VIS));

        if (vis == Visibility.PRIVATE) {
            // 비공개면 본인만 볼 수 있음
            if (!timetable.getMember().getId().equals(currentUser.getId())) {
                throw new TimetablePrivateException();
            }
        }
    }

    private Timetable loadTimetableWithItems(Long timetableId) {
        // 1번 쿼리: Timetable + items
        Timetable timetable = timetableRepository.findWithItemsById(timetableId)
                .orElseThrow(TimetableNotFoundException::new);

        // 2번 쿼리: 같은 영속성 컨텍스트 내에서 schedules 로드
        timetableItemRepository.findWithSchedulesByTimetableId(timetableId);

        // 영속성 컨텍스트가 자동으로 연결해줌
        return timetable;
    }

    private TimetableSharedViewResponse toSharedViewResponse(Timetable tt) {
        List<TimetableItemResponse> items = tt.getItems().stream()
                .map(TimetableItemResponse::of)
                .toList();
        return new TimetableSharedViewResponse(
                tt.getId(), tt.getYear(), tt.getSemester(),
                tt.getMember().getName(), items
        );
    }
}
