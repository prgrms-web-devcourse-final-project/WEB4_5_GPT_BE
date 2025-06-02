import http from "k6/http";
import {check} from "k6";
import {Counter} from "k6/metrics";

// —— 테스트 파라미터 ——
const COURSE_ID = 11;
const BASE = "https://api.un1hub.site";

// 테스트할 단일 학생 계정
const STUDENT = {
    email: "concurrencyStudent1@auni.ac.kr",
    password: "studentPw",
};

// 성공/실패 카운터
export let cancelSuccess = new Counter("cancel_success");
export let cancelFail = new Counter("cancel_fail");
export let loginFail = new Counter("login_fail");

export let options = {
    vus: 1,         // 단일 VU
    iterations: 1,  // 1회 호출
};

export default function () {
    // 1) 로그인
    const loginRes = http.post(
        `${BASE}/api/members/login`,
        JSON.stringify(STUDENT),
        {headers: {"Content-Type": "application/json"}}
    );
    check(loginRes, {"login 200": (r) => r.status === 200});
    if (loginRes.status !== 200) {
        loginFail.add(1);
        return;
    }
    const token = loginRes.json("data.accessToken");

    // 2) 수강 취소: DELETE /api/enrollments/{courseId}
    const cancelRes = http.del(
        `${BASE}/api/enrollments/${COURSE_ID}`,
        null,
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );
    if (cancelRes.status >= 200 && cancelRes.status < 300) {
        cancelSuccess.add(1);
    } else {
        cancelFail.add(1);
    }

    // 검증 로그 (선택)
    check(cancelRes, {"cancel 2xx": (r) => r.status >= 200 && r.status < 300});
}
// cd /home/ec2-user/scripts
/**
 docker run --rm \
 --name k6_prometheus \
 -v "$(pwd)":/scripts \
 -w /scripts \
 -p 6565:6565 \
 -e K6_PROMETHEUS_HOST=0.0.0.0 \
 -e K6_PROMETHEUS_PORT=6565 \
 grafana/k6:latest run \
 --out experimental-prometheus-rw=0.0.0.0:6565 \
 student_cancel.js
 */