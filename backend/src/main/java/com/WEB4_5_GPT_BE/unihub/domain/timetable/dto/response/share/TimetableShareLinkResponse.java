package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share;

import java.time.LocalDateTime;

public record TimetableShareLinkResponse(
        String shareUrl,
        LocalDateTime expiresAt
) {}