package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel;

public record EnrollmentCancelCommand(
        Long studentId,
        Long courseId
) {
}