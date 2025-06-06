package com.WEB4_5_GPT_BE.unihub.domain.university.service;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.UniversityRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityService {

    private final UniversityRepository universityRepository;

    /**
     * 대학 목록 조회 (전체)
     */
    public List<UniversityResponse> getAllUniversities() {
        return universityRepository.findAll().stream()
                .map(UniversityResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 대학 단건 조회
     */
    public UniversityResponse getUniversity(Long universityId) {
        University university = findUniversityById(universityId);
        return UniversityResponse.from(university);
    }

    /**
     * 대학 생성
     */
    @Transactional
    public UniversityResponse createUniversity(UniversityRequest request) {
        // 존재하는 대학 명인지 확인
        if (universityRepository.existsByName(request.name())) {
            throw new UnihubException("409", "이미 존재하는 대학 이름입니다.");
        }

        // 존재하는 이메일 도메인인지 확인
        if (universityRepository.existsByEmailDomain(request.emailDomain())) {
            throw new UnihubException("409", "이미 존재하는 이메일 도메인입니다.");
        }

        University university = University.builder()
                .name(request.name())
                .emailDomain(request.emailDomain())
                .build();

        University savedUniversity = universityRepository.save(university);
        return UniversityResponse.from(savedUniversity);
    }

    /**
     * 대학 수정
     */
    @Transactional
    public UniversityResponse updateUniversity(Long universityId, UniversityRequest request) {
        University university = findUniversityById(universityId);

        // 이름 중복 확인 (현재 대학과 다른 경우에만 중복 검사)
        if (!university.getName().equals(request.name())
                && universityRepository.existsByName(request.name())) {
            throw new UnihubException("409", "이미 존재하는 대학 이름입니다.");
        }

        // 이메일 도메인 중복 확인 (현재 대학과 다른 경우에만 중복 검사)
        if (!university.getEmailDomain().equals(request.emailDomain())
                && universityRepository.existsByEmailDomain(request.emailDomain())) {
            throw new UnihubException("409", "이미 존재하는 이메일 도메인입니다.");
        }

        university.setName(request.name());
        university.setEmailDomain(request.emailDomain());
        return UniversityResponse.from(university);
    }

    /**
     * 대학 삭제
     */
    @Transactional
    public void deleteUniversity(Long universityId) {
        University university = findUniversityById(universityId);
        universityRepository.delete(university);
    }

    /**
     * 대학 ID로 조회 (없으면 예외 발생)
     */
    public University findUniversityById(Long universityId) {
        return universityRepository
                .findById(universityId)
                .orElseThrow(() -> new UnihubException("404", "해당 대학이 존재하지 않습니다."));
    }
}
