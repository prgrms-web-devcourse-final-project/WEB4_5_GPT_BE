package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Visibility;
import jakarta.validation.constraints.NotNull;

public record TimetableShareLinkRequest(
        @NotNull(message = "시간표 ID는 필수입니다.")
        Long timetableId,

        @NotNull(message = "공개 범위를 선택해주세요.")
        Visibility visibility
) {}