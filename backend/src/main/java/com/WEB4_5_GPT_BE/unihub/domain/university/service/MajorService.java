package com.WEB4_5_GPT_BE.unihub.domain.university.service;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;

public interface MajorService {
  Major getMajor(Long universityId, Long majorId);
}
