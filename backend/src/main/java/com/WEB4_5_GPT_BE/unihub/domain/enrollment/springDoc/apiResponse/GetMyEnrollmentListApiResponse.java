package com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 내 수강신청 목록 조회 API의 공통 응답 예시를 정의하는 메타 어노테이션입니다.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "내 수강목록 조회 성공",
                content = @Content(
                        array = @ArraySchema(schema = @Schema(implementation = MyEnrollmentResponse.class))
//                        ,examples = @ExampleObject(
//                                value = """
//                                        {
//                                          "code": "200",
//                                          "message": "내 수강목록 조회가 완료되었습니다.",
//                                          "data": [
//                                            {
//                                              "courseId": 1,
//                                              "courseName": "자료구조",
//                                              "credit": 3,
//                                              "location": "OO동 401호",
//                                              "schedules": [
//                                                { "dayOfWeek": "MON", "startTime": "09:00:00", "endTime": "10:30:00" }
//                                              ]
//                                            }
//                                          ]
//                                        }
//                                        """
//                        )
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "AccessToken이 만료된 경우"
                , content = @Content(
                examples = @ExampleObject(
                        value = """
                                {
                                  "code": "401-1",
                                  "message": "AccessToken이 만료되었습니다.",
                                  "data": {}
                                }
                                """
                )
        )
        ),
})
public @interface GetMyEnrollmentListApiResponse {
}