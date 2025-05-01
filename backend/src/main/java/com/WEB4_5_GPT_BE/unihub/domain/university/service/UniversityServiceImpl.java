//package com.WEB4_5_GPT_BE.unihub.domain.university.service;
//
//import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
//import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
//import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class UniversityServiceImpl implements UniversityService {
//
//  private final UniversityRepository universityRepository;
//
//  @Override
//  public University getUniversity(Long universityId) {
//    return universityRepository
//        .findById(universityId)
//        .orElseThrow(() -> new UnihubException("404", "존재하지 않는 대학입니다."));
//  }
//}
