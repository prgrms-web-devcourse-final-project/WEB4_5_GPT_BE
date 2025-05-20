# 🏫 UniHub

프로그래머스 4기 5회차 **GPT야 해줘 팀**의 백엔드 프로젝트입니다.

### 👥 Member

|                **서준식**                 |                   **옥정현**                   |                   **정성철**                   |                 **강을찬**                  |                 **박주원**                  |
| :-----------------------------------------: | :-----------------------------------------: | :-----------------------------------------: | :-----------------------------------------: | :-----------------------------------------: |
| <img src="https://github.com/sojunsik.png" width="200"> | <img src="https://github.com/okjunghyeon.png" width="200"> | <img src="https://github.com/jsc5023.png" width="200"> | <img src="https://github.com/thegreatkang.png" width="200"> | <img src="https://github.com/SalinatedCoffee.png" width="200"> |
|                 **PO**                 |                     **BE-L**                     |                     **BE**                     |                   **BE**                    |                   **BE**                    |
|   [GitHub](https://github.com/sojunsik)    |   [GitHub](https://github.com/okjunghyeon)    |   [GitHub](https://github.com/jsc5023)    |   [GitHub](https://github.com/thegreatkang)    |   [GitHub](https://github.com/SalinatedCoffee)    |


---

### 📌 1. 프로젝트명
**UniHub — 대학교 통합 관리 서비스**


### 📚 2. 프로젝트 소개
> **UniHub**는 분산된 대학 생활 서비스를 하나의 통합 플랫폼에서 관리할 수 있도록 지원합니다.  
> 수강 신청, 캠퍼스 활동, 전공 관리 등 다양한 기능을 제공하여 학생과 교직원의 대학 생활을 쉽고 편리하게 만듭니다.

### 🚀 3. 주요 기능
- [ ] 수강신청 기능
- [ ] 강의/시간표 관리
- [ ] 전공 변경 기능
- [ ] 회원가입 / 로그인 (이메일 인증)
- [ ] 교수·학생 역할 관리
- [ ] 알림 시스템
- [ ] 마이페이지 (개인정보 수정, 비밀번호 변경)
- [ ] 관리자 기능 (회원 관리, 수강신청 기간 설정 등)

## 🛠 기술 스택

### 💻 Language
![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)

### ⚙ Framework & Library
![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/SpringSecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/SpringDataJPA-6DB33F?style=for-the-badge&logo=hibernate&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Springdoc OpenAPI](https://img.shields.io/badge/Springdoc%20OpenAPI-68B5F4?style=for-the-badge&logo=swagger&logoColor=white)

### 🗄 Database
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![H2](https://img.shields.io/badge/H2-00599C?style=for-the-badge&logo=h2&logoColor=white)

### 🛠 Development Tools
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white)
![Visual Studio Code](https://img.shields.io/badge/VSCode-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![NGINX](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![Amazon S3](https://img.shields.io/badge/AmazonS3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![AWS ECS](https://img.shields.io/badge/AWS%20ECS-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)

### 🔧 Collaboration Tools
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)
![Slack](https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white)
![ZEP](https://img.shields.io/badge/ZEP-6001D2?style=for-the-badge&logo=zepeto&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)

---
## 📄 추가 문서

### 🔗 ERD
<details>
  <summary>ERD 보기</summary>
![unihub](https://github.com/user-attachments/assets/632b7962-3dba-4d7a-b76b-76b3720f1c58)
<details>

### 📜 프로젝트 기획서
- [프로젝트 기획서 바로가기](https://www.notion.so/1db3550b7b558190b279c1b3b4a32d16?pvs=4)

### 📌 API 명세서
- [API 명세서 바로가기](https://www.notion.so/API-1db3550b7b55814d99b8cdf2a3ffb242?pvs=4)

### 플로우 차트

<details>
  <summary>플로우 차트 보기</summary>

  ```mermaid
  ---
  config:
    layout: dagre
  ---
  flowchart TB

    %% ────────────────────────────────────────
    %% 1행: 로그인 & 회원가입
    subgraph row1["row1"]
      direction LR
      subgraph s2["로그인"]
        direction TB
        L1["👤 사용자"]
        L2["로그인 요청"]
        L3["DTO 바인딩"]
        L4["입력값 검증"]
        L5["로그인 실패 횟수 확인"]
        L6["회원 정보 조회"]
        L7["비밀번호 검증"]
        L8["교수 승인 상태 확인"]
        L9["토큰 생성"]
        L10["토큰 응답 반환"]
        %% 내부 흐름
        L1 --> L2 --> L3 --> L4
        L4 --> L5 & L6
        L6 --> L7 --> L8 --> L9 --> L10
      end

      subgraph s1["회원가입"]
        direction TB
        S1["👤 사용자"]
        S2["회원가입 요청"]
        S3["DTO 바인딩"]
        S4["입력값 유효성 검증"]
        S5["이메일 인증 확인 및 도메인 검사"]
        S6["학교·전공 정보 조회"]
        S7["이메일·학번 중복 검사"]
        S8["회원 정보 저장"]
        S9["회원가입 완료 응답"]
        %% 내부 흐름
        S1 --> S2 --> S3 --> S4
        S4 --> S5 & S7
        S5 --> S6 --> S8
        S7 --> S8 --> S9
      end
    end

    %% ────────────────────────────────────────
    %% 2행: 마이페이지
    subgraph row2["row2"]
      direction LR
      subgraph s6["마이페이지"]
        direction TB
        M1["로그인 검증 완료"]
        M2{"사용자 유형 분기"}
        %% 분기 흐름
        M1 --> M2

        subgraph s3["학생 화면"]
          direction TB
          M3["학생 마이페이지 화면"]
          M3a["기본 정보 조회"]
          M3b["전공 변경 버튼"]
          M3c["비밀번호 변경 버튼"]
          M3d["탈퇴 버튼"]
          %% 내부 흐름
          M3 --> M3a
          M3 --> M3b
          M3 --> M3c
          M3 --> M3d
        end

        subgraph s4["교수 화면"]
          direction TB
          M4["교수 마이페이지 화면"]
          M4a["기본 정보 조회"]
          M4b["강의 목록 조회"]
          M4c["비밀번호 변경 버튼"]
          M4d["탈퇴 버튼"]
          %% 내부 흐름
          M4 --> M4a
          M4 --> M4b
          M4 --> M4c
          M4 --> M4d
        end

        subgraph s5["관리자 화면"]
          direction TB
          M5["관리자 마이페이지 화면"]
          M5a["기본 정보 조회"]
          M5c["비밀번호 변경 버튼"]
          M5d["탈퇴 버튼"]
          %% 내부 흐름
          M5 --> M5a
          M5 --> M5c
          M5 --> M5d
        end

        %% 분기 연결
        M2 -- 학생 --> M3
        M2 -- 교수 --> M4
        M2 -- 관리자 --> M5
      end
    end

    %% ────────────────────────────────────────
    %% 3행: 관리자 도메인 (세로로 쌓기)
    subgraph row3["관리자 도메인"]
      direction TB
      subgraph s7["사용자 목록 조회"]
        direction TB
        A1["관리자 로그인"]
        A2["회원/권한 관리 클릭"]
        A3["사용자 목록 조회 API 호출"]
        A4["목록 테이블 표시\n(페이징·검색)"]
        A1 --> A2 --> A3 --> A4
      end

      subgraph s8["수강신청 기간 조회"]
        direction TB
        B1["수강신청 관리 메뉴 클릭"]
        B2["시작일·종료일 선택"]
        B3["조회 버튼 클릭"]
        B4{"기간 유효성 검증"}
        B5["기간 목록에 반영"]
        B6["시작일보다 종료일이 커야합니다 알림"]
        B1 --> B2 --> B3 --> B4
        B4 -- 정상 --> B5
        B4 -- 오류 --> B6
      end

      subgraph s9["수강신청 기간 등록"]
        direction TB
        C1["수강신청 관리 메뉴 클릭"]
        C2["등록 클릭"]
        C3["등록 정보 입력"]
        C4["등록 버튼 클릭"]
        C5{"유효성 검사"}
        C6["등록 완료"]
        C7["이미 등록된 학교입니다 알림"]
        C8["날짜 형식 오류 알림"]
        C1 --> C2 --> C3 --> C4 --> C5
        C5 -- 정상 --> C6
        C5 -- 중복 --> C7
        C5 -- 날짜 오류 --> C8
      end

      subgraph s10["교직원 가입 승인"]
        direction TB
        D1_교직원["교직원 등록 신청 알림"]
        D2_교직원["교직원 목록 조회"]
        D3_교직원["상세 정보 확인"]
        D4_교직원["승인/거절 클릭"]
        D5_교직원["상태 변경 처리"]
        D6_교직원["이메일로 알림 전송"]
        D1_교직원 --> D2_교직원 --> D3_교직원 --> D4_교직원 --> D5_교직원 --> D6_교직원
      end

      subgraph s11["관리자 초대"]
        direction TB
        E1["관리자 초대 페이지 클릭"]
        E2["초대 이메일 입력"]
        E3["초대 버튼 클릭"]
        E4["임시 계정 생성 & 메일 발송"]
        E5["수신자 로그인 & 비밀번호 변경"]
        E1 --> E2 --> E3 --> E4 --> E5
      end
    end

    %% ────────────────────────────────────────
    %% 4행: 공지사항
    subgraph row4["row4"]
      direction LR
      subgraph s14["공지사항"]
        direction TB
        subgraph s14a["읽기 흐름"]
          direction TB
          N1["공지사항 목록 조회"]
          N2["목록 표시\n(제목·본문·첨부파일)"]
          N3["상세 조회 클릭"]
          N4["상세 정보 표시\n(제목·본문·첨부파일)"]
          N1 --> N2 --> N3 --> N4
        end
        subgraph s14b["관리 흐름"]
          direction TB
          M21["공지사항 관리 메뉴 클릭"]
          M22["새 공지 등록 클릭"]
          M23["제목·본문 작성\n(첨부파일 선택)"]
          M24["작성 요청"]
          M25["공지 생성 완료"]
          M26["목록/상세에서 수정 클릭"]
          M27["수정 폼 표시"]
          M28["변경 내용 입력"]
          M29["수정 요청"]
          M30["공지 업데이트 완료"]
          M31["목록/상세에서 삭제 클릭"]
          M32["삭제 확인 팝업"]
          M33["삭제 요청"]
          M34["공지 소프트 삭제 완료"]
          M21 --> M22 --> M23 --> M24 --> M25
          M26 --> M27 --> M28 --> M29 --> M30
          M31 --> M32 --> M33 --> M34
        end
      end
    end

    %% ────────────────────────────────────────
    %% 5행: 수강신청 & 강의 목록 조회
    subgraph row5["row5"]
      direction LR
      subgraph s16["수강신청"]
        direction TB
        Q1["강의 목록에서 과목 선택"]
        Q2["수강 신청 버튼 클릭"]
        Q3{"수강신청 기간 확인"}
        Q4{"정원 확인"}
        Q5{"시간표 충돌 확인"}
        Q6{"학점 한도 확인"}
        Q7["수강신청 성공<br>신청 완료 알림"]
        Q8["오류 알림"]
        Q1 --> Q2 --> Q3
        Q3 -- 기간 아님 --> Q8
        Q3 -- 기간 내 --> Q4
        Q4 -- 정원 초과 --> Q8
        Q4 -- 가능 --> Q5
        Q5 -- 충돌 발생 --> Q8
        Q5 -- 충돌 없음 --> Q6
        Q6 -- 학점 초과 --> Q8
        Q6 -- 한도 내 --> Q7
      end

      subgraph s15["강의 목록 조회"]
        direction TB
        P1["메인페이지 → 강의 목록 페이지 이동"]
        P2["필터 설정 → (학기, 학과, 교수명, 요일, 시간)"]
        P3["강의 목록 조회 API 호출"]
        P4["검색 결과 표시<br>(강의명, 교수, 시간, 강의실, 잔여인원)"]
        P5{"결과가 있나요?"}
        P6["강의계획서 보기 클릭"]
        P7["검색 결과가 없습니다 메시지"]
        P1 --> P2 --> P3 --> P4 --> P5
        P5 -- 예 --> P6
        P5 -- 아니오 --> P7
      end
    end

    %% ────────────────────────────────────────
    %% 6행: 시간표 기능
    subgraph row6["row6: 시간표 기능"]
      direction LR
      subgraph s17["시간표 조회"]
        direction TB
        T1["내 시간표 메뉴 클릭"]
        T2["시간표 API 호출 & 캘린더 렌더링"]
        T3{"수업 데이터 존재?"}
        T4["캘린더에 수업 블럭 표시"]
        T5["빈 시간표 표시"]
      end
      subgraph s18["수업 추가 (수동)"]
        direction TB
        U1["강의 추가 버튼 클릭"]
        U2["검색창에 강의명/교수 입력"]
        U3["검색 API 호출"]
        U4{"검색 결과 선택?"}
        U5["[추가] 클릭 → 추가 API 요청"]
        U6["추가 취소"]
        U7["성공: 캘린더 반영"]
        U8["오류: 이미 등록된 강의"]
      end
      subgraph s19["수강 강의 반영"]
        direction TB
        R1["수강 반영 버튼 클릭"]
        R2["내 수강 목록 API 호출"]
        R3["자동 배치 로직 실행"]
        R4["성공: n개 강의 반영 메시지"]
        R5["예외: 신청 강의 없음 메시지"]
      end
      subgraph s20["수업 삭제"]
        direction TB
        TD1["삭제할 강의 블럭 클릭"]
        TD2["삭제 확인 팝업"]
        TD3{"삭제 진행?"}
        TD4["삭제 API 요청"]
        TD5["삭제 취소"]
        TD6["성공: 캘린더에서 제거"]
        TD7["오류: 삭제 실패 메시지"]
        TD1 --> TD2 --> TD3
        TD3 -- 예 --> TD4 --> TD6
        TD3 -- 아니오 --> TD5
        TD4 -- 오류 --> TD7
      end
      subgraph s21["시간표 공유"]
        direction TB
        S1["공유 버튼 클릭"]
        S2["링크/이미지 생성 API 요청"]
        S3["링크 복사 또는 이미지 저장"]
        S4{"예외 상황?"}
        S5["공유 불가 메시지"]
        S1 --> S2 --> S3 --> S4
        S4 -- 오류 --> S5
      end
      subgraph s22["메모"]
        direction TB
        X1["수업 블럭 클릭"]
        X2["메모 편집 팝업 띄우기"]
        X3["메모 입력 후 저장 클릭"]
        X4{"입력 유효?"}
        X5["캘린더에 메모 표시"]
        X6["입력 필요/길이 초과 경고"]
        X1 --> X2 --> X3 --> X4
        X4 -- 유효 --> X5
        X4 -- 오류 --> X6
      end
    end



