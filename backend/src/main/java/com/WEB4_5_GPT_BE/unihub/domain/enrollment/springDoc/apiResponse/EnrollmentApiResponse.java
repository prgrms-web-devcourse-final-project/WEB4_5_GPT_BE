package com.WEB4_5_GPT_BE.unihub.domain.enrollment.springDoc.apiResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 수강 신청 API의 공통 응답 예시를 정의하는 메타 어노테이션입니다.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "수강 신청 성공",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "200",
                                          "message": "수강 신청이 완료되었습니다.",
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
                description = "강좌 정보가 없는 경우",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "404",
                                          "message": "강좌를 찾을 수 없습니다.",
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
                responseCode = "409",
                description = "신청하려는 강의 정원 초과 시",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "409",
                                          "message": "정원이 초과되어 수강 신청이 불가능합니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = "동일 강좌 중복 신청 시",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "409",
                                          "message": "이미 신청한 강의입니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = "최대 학점(21)을 초과하여 수강 신청하는 경우",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "409",
                                          "message": "학점 한도를 초과하여 수강신청할 수 없습니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = " 기존 신청한 강좌와 시간표가 겹치는 경우",
                content = @Content(
                        examples = @ExampleObject(
                                value = """
                                        {
                                          "code": "409",
                                          "message": "기존 신청한 강좌와 시간표가 겹칩니다.",
                                          "data": []
                                        }
                                        """
                        )
                )
        )
})
public @interface EnrollmentApiResponse {
}
