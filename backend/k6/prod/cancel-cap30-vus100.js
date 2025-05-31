import http from "k6/http";
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
export let cancelSuccess = new Counter("cancel_success");
export let cancelFail = new Counter("cancel_fail");
export let loginFail = new Counter("login_fail");

export let options = {
    scenarios: {
        cancelTest: {
            executor: "per-vu-iterations",
            vus: VUS,
            iterations: 1,
            maxDuration: "30s",
        },
    },
    thresholds: {
        // 100건 중 100건 2xx 기대
        "cancel_success": ["count == 30"],
        "cancel_fail": ["count == 0"],
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
    if (loginRes.status !== 200) {
        loginFail.add(1);
        return;
    }
    const token = loginRes.json("data.accessToken");

    // 2) 수강 취소 요청
    let res = http.del(
        `${BASE}/api/enrollments/${COURSE_ID}`,
        null,
        {
            headers: {
                "Authorization": `Bearer ${token}`,
            },
        }
    );
    if (res.status >= 200 && res.status < 300) {
        cancelSuccess.add(1);
    } else {
        cancelFail.add(1);
    }
}

// cd /home/ec2-user/scripts
// docker run --rm -v "$(pwd)":/scripts grafana/k6:latest run /scripts/cancel-cap30-vus100.js