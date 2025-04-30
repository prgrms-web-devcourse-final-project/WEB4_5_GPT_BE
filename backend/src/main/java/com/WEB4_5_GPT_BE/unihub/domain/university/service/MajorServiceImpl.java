package com.WEB4_5_GPT_BE.unihub.domain.university.service;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MajorServiceImpl implements MajorService {

  private final MajorRepository majorRepository;

  @Override
  public Major getMajor(Long universityId, Long majorId) {
    return majorRepository
        .findByIdAndUniversityId(majorId, universityId)
        .orElseThrow(() -> new UnihubException("404", "존재하지 않는 전공입니다."));
  }
}
