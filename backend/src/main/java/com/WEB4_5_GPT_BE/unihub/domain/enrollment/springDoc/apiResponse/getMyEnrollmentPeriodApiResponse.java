package com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 내 수강신청 기간 조회 API의 공통 응답 예시를 정의하는 메타 어노테이션입니다.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "내 수강신청 기간 정보를 조회하고 현재 해당기간에 포함되는지 확인합니다.",
                content = @Content(
                        examples = {
                                @ExampleObject(
                                        name = "수강신청 기간 존재 and 현재 열림",
                                        value = """
                                                {
                                                  "code": "200",
                                                  "message": "내 수강신청 기간 정보를 조회했습니다.",
                                                  "data": {
                                                    "studentId": 5,
                                                    "universityName": "A대학교",
                                                    "year": 2025,
                                                    "grade": 1,
                                                    "semester": 1,
                                                    "startDate": "2025-05-01",
                                                    "endDate": "2025-05-30",
                                                    "isEnrollmentOpen": true
                                                  }
                                                }
                                                """
                                ),
                                @ExampleObject(
                                        name = "수강신청 기간 존재 but 현재 닫힘",
                                        value = """
                                                {
                                                  "code": "200",
                                                  "message": "내 수강신청 기간 정보를 조회했습니다.",
                                                  "data": {
                                                    "studentId": 5,
                                                    "universityName": "A대학교",
                                                    "year": 2025,
                                                    "grade": 2,
                                                    "semester": 1,
                                                    "startDate": "2025-06-01",
                                                    "endDate": "2025-06-30",
                                                    "isEnrollmentOpen": false
                                                  }
                                                }
                                                """
                                ),
                                @ExampleObject(
                                        name = "수강신청 기간 정보 없음",
                                        value = """
                                                {
                                                  "code": "200",
                                                  "message": "내 수강신청 기간 정보를 조회했습니다.",
                                                  "data": {
                                                    "studentId": null,
                                                    "universityName": null,
                                                    "year": null,
                                                    "grade": null,
                                                    "semester": null,
                                                    "startDate": null,
                                                    "endDate": null,
                                                    "isEnrollmentOpen": false
                                                  }
                                                }
                                                """
                                )
                        }
                )
        )
})
public @interface getMyEnrollmentPeriodApiResponse {
}
