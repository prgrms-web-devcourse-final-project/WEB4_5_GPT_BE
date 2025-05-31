import http from "k6/http";
import {check} from "k6";
import {SharedArray} from "k6/data";
import {Counter} from "k6/metrics";

// —— 테스트 파라미터 ——
const VUS = 100;
const COURSE_ID = 11;
const BASE = "https://api.un1hub.site";

// students.json 로드 (100개 계정)
const users = new SharedArray("students", () =>
    JSON.parse(open("students.json"))
);

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
        // 30건 2xx 성공, 70건 비2xx 실패
        "enroll_success": ["count == 30"],
        "enroll_fail": ["count == 70"],
    },
};

export default function () {
    // 각 VU마다 한 번씩 실행
    const user = users[(__VU - 1) % users.length];

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

// cd /home/ec2-user/scripts
// docker run --rm -v "$(pwd)":/scripts grafana/k6:latest run /scripts/enroll-cap30-vus100.js