package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record ProfessorApprovalRequest(
        @NotNull ApprovalStatus approvalStatus
) {
}
