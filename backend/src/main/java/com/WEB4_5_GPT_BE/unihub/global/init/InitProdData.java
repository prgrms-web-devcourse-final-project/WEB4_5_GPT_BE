package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"prod", "dev"}) // 개발, 운영 배포 환경에서만 실행
@RequiredArgsConstructor
public class InitProdData {

    private final InitDataHelper helper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        if (helper.countMembers() > 0) {
            helper.ensureProfessorApprovalStatus("professor@auni.ac.kr", ApprovalStatus.APPROVED);
            return;
        }

        University university = helper.createUniversity("A대학교");
        Major major = helper.createMajor("소프트웨어전공", university);

        helper.createStudent("haneulkim@auni.ac.kr", "studentPw", "김하늘", "20250001",
                university.getId(), major.getId());

        helper.createProfessor("professor@auni.ac.kr", "password", "김교수", "EMP20250001",
                university.getId(), major.getId(), ApprovalStatus.APPROVED);

        helper.createAdmin("adminmaster@auni.ac.kr", "adminPw", "최고관리자", passwordEncoder);
    }
}
