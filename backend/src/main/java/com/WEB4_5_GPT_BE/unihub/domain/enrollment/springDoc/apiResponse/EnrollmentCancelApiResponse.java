package com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 수강 취소 API의 공통 응답 예시를 정의하는 메타 어노테이션입니다.
 * 모든 응답에 data 필드를 빈 리스트로 포함합니다.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "수강 취소가 완료되었습니다.",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "200",
                                          "message": "수강 취소가 완료되었습니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "401-1",
                description = "AccessToken이 만료된 경우",
                content = @Content(
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
        @ApiResponse(
                responseCode = "404",
                description = "수강신청 기간 정보가 없는 경우",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "404",
                                          "message": "수강신청 기간 정보가 없습니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "403",
                description = "수강신청 기간 외 요청인 경우",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "403",
                                          "message": "현재 수강신청 기간이 아닙니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "404",
                description = "수강신청 내역을 찾을 수 없음",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "404",
                                          "message": "수강신청 내역이 존재하지 않습니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        )
})
public @interface EnrollmentCancelApiResponse {
}
