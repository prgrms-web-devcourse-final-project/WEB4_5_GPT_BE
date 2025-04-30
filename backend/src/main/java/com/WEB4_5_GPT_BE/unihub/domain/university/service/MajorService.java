package com.WEB4_5_GPT_BE.unihub.domain.university.service;

import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.MajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MajorService {

    private final MajorRepository majorRepository;
    private final UniversityRepository universityRepository;

    /**
     * 모든 전공 목록 조회 (전체)
     */
    public List<MajorResponse> getAllMajors() {
        return majorRepository.findAll().stream().map(MajorResponse::from).collect(Collectors.toList());
    }

    /**
     * 특정 대학의 전공 목록 조회 (전체)
     */
    public List<MajorResponse> getMajorsByUniversity(Long universityId) {
        University university = universityRepository.getReferenceById(universityId);
        return majorRepository.findByUniversity(university).stream()
                .map(MajorResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전공 등록
     */
    @Transactional
    public MajorResponse createMajor(MajorRequest request) {
        University university = universityRepository.getReferenceById(request.universityId());

        // 이름 중복 확인
        if (majorRepository.existsByUniversityAndName(university, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 전공 이름입니다.");
        }

        Major major = Major.builder().university(university).name(request.name()).build();

        Major savedMajor = majorRepository.save(major);
        return MajorResponse.from(savedMajor);
    }

    /**
     * 전공 정보 수정
     */
    @Transactional
    public MajorResponse updateMajor(Long majorId, MajorRequest request) {
        Major major = findMajorById(majorId);
        University university = universityRepository.getReferenceById(request.universityId());

        // 이름 중복 확인 (현재 전공과 다른 경우에만 중복 검사)
        if (!major.getName().equals(request.name())
                && majorRepository.existsByUniversityAndName(major.getUniversity(), request.name())) {
            throw new IllegalArgumentException("이미 존재하는 전공 이름입니다.");
        }

        major.setUniversity(university);
        major.setName(request.name());
        return MajorResponse.from(major);
    }

    /**
     * 전공 삭제
     */
    @Transactional
    public void deleteMajor(Long majorId) {
        Major major = findMajorById(majorId);
        majorRepository.delete(major);
    }

    /**
     * 전공 ID로 조회 (없으면 예외 발생)
     */
    private Major findMajorById(Long majorId) {
        return majorRepository
                .findById(majorId)
                .orElseThrow(() -> new IllegalArgumentException("해당 전공이 존재하지 않습니다."));
    }
}
