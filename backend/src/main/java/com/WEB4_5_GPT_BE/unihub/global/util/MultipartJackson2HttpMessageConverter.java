package com.WEB4_5_GPT_BE.unihub.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

/**
 * HTTP multipart/form-data 요청에서 JSON 파트를 application/octet-stream으로 전송하더라도
 * Jackson 메시지 컨버터를 통해 객체로 바인딩할 수 있도록 지원하는 HTTP 메시지 컨버터입니다.
 * <p>
 * Spring 기본 컨버터는 multipart 파일 업로드 시 JSON 파트가 application/octet-stream으로 처리되면
 * 자동으로 JSON으로 파싱하지 않으므로, 이 컨버터를 추가하여 해당 문제를 해결합니다.
 */
@Component
public class MultipartJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {

    /**
     * ObjectMapper를 주입받아 application/octet-stream 미디어 타입을 처리 대상으로 설정합니다.
     *
     * @param objectMapper JSON 파싱을 위한 ObjectMapper
     */
    public MultipartJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper, MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * 쓰기 기능은 지원하지 않으므로 항상 false를 반환합니다.
     */
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    /**
     * 쓰기 기능은 지원하지 않으므로 항상 false를 반환합니다.
     */
    @Override
    public boolean canWrite(Type type, Class<?> contextClass, MediaType mediaType) {
        return false;
    }

    /**
     * 응답 바디 직렬화를 위한 쓰기 지원도 비활성화합니다.
     */
    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }
}
