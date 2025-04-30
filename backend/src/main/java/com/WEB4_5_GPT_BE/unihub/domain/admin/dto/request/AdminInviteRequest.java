package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminInviteRequest(@NotNull String adminName, @NotNull String email) {}
