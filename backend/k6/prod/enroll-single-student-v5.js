import http from "k6/http";
import {Counter} from "k6/metrics";

// —— 테스트 파라미터 ——
// 한 학생이 동시에 보낼 요청 수
const VUS = 5;
const ITERATIONS = 5;
const COURSE_ID = 11;
const BASE = "https://api.un1hub.site";

// 테스트할 단일 학생 계정
const STUDENT = {
    email: "concurrencyStudent1@auni.ac.kr",
    password: "studentPw",
};

// 성공/실패 카운터
export let enrollSuccess = new Counter("enroll_success");
export let enrollFail = new Counter("enroll_fail");
export let loginFail = new Counter("login_fail");

export let options = {
    scenarios: {
        singleStudentTest: {
            executor: "shared-iterations",
            vus: VUS,              // 동시에 5 VU
            iterations: ITERATIONS,// 총 5회 반복
            maxDuration: "10s",
        },
    },
    thresholds: {
        // 1번만 성공, 4번은 실패를 기대
        "enroll_success": ["count == 1"],
        "enroll_fail": ["count == 4"],
    },
};

export default function () {
    // 1) 로그인
    let loginRes = http.post(
        `${BASE}/api/members/login`,
        JSON.stringify(STUDENT),
        {headers: {"Content-Type": "application/json"}}
    );
    if (loginRes.status !== 200 || !loginRes.json("data.accessToken")) {
        loginFail.add(1);
        return;
    }
    const token = loginRes.json("data.accessToken");

    // 2) 수강신청
    let enrollRes = http.post(
        `${BASE}/api/enrollments`,
        JSON.stringify({courseId: COURSE_ID}),
        {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (enrollRes.status >= 200 && enrollRes.status < 300) {
        enrollSuccess.add(1);
    } else {
        enrollFail.add(1);
    }
}

// cd /home/ec2-user/scripts
// docker run --rm -v "$(pwd)":/scripts grafana/k6:latest run /scripts/enroll-single-student-v5.js