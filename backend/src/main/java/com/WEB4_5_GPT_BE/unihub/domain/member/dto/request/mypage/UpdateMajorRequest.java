package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage;

import jakarta.validation.constraints.NotNull;

public record UpdateMajorRequest(
        @NotNull Long majorId
) {}