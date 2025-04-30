package com.WEB4_5_GPT_BE.unihub.domain.university.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;

public record UniversityResponse(Long id, String name) {
  public static UniversityResponse from(University university) {
    return new UniversityResponse(university.getId(), university.getName());
  }
}
