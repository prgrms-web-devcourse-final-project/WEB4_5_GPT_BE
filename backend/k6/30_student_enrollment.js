import http from "k6/http";
import {check} from "k6";
import {SharedArray} from "k6/data";
import {Counter} from "k6/metrics";

// —— 테스트 파라미터 ——
const VUS = 30;  // 30명의 학생만
const COURSE_ID = 11;
const BASE = __ENV.BASE || "http://host.docker.internal:8080";

// students.json 로드 (100개 계정)
const allUsers = new SharedArray("students", () =>
    JSON.parse(open("students.json"))
);

// 앞에서부터 30명만 사용
const users = allUsers.slice(0, VUS);

// 성공/실패 카운터
export let enrollSuccess = new Counter("enroll_success");
export let enrollFail = new Counter("enroll_fail");

export let options = {
    scenarios: {
        enrollmentTest: {
            executor: "per-vu-iterations",
            vus: VUS,
            iterations: 1,
            maxDuration: "30s",
        },
    },
    thresholds: {
        // 30명 중 30명은 성공, 0명은 실패 기대
        "enroll_success": ["count == 30"],
        "enroll_fail": ["count == 0"],
    },
};

export default function () {
    // VU 번호에 맞춰 0..29 인덱스 사용자 선택
    const user = users[__VU - 1];

    // 1) 로그인
    let loginRes = http.post(
        `${BASE}/api/members/login`,
        JSON.stringify({email: user.email, password: user.password}),
        {headers: {"Content-Type": "application/json"}}
    );
    check(loginRes, {"login 200": r => r.status === 200});
    if (loginRes.status !== 200) {
        enrollFail.add(1);
        return;
    }
    const token = loginRes.json("data.accessToken");

    // 2) 비동기 수강신청 요청
    let res = http.post(
        `${BASE}/api/enrollments`,
        JSON.stringify({courseId: COURSE_ID}),
        {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (res.status >= 200 && res.status < 300) {
        enrollSuccess.add(1);
    } else {
        enrollFail.add(1);
    }
}


// 실행 방법
// 1. Docker 환경에서 실행
// 2. c: 경로에 js 파일과 students.json 파일을 저장
// 3. 해당 경로로 이동하여 powershell에서 아래 명령어 실행
// docker run --rm --name k6_1 --network common -v ${pwd}:/scripts -w /scripts grafana/k6:latest run 30_student_enrollment.js