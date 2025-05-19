package com.WEB4_5_GPT_BE.unihub.global.exception;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.AccessTokenExpiredException;
import com.WEB4_5_GPT_BE.unihub.global.alert.AlertNotifier;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AlertNotifier alertNotifier;
    private final HttpServletRequest request;

    @ExceptionHandler(UnihubException.class)
    public ResponseEntity<RsData<Void>> handleUnihubException(UnihubException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new RsData<>(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>("400", message.isBlank() ? "잘못된 요청입니다." : message, null));
    }

    @ExceptionHandler({ AuthenticationException.class, AccessDeniedException.class })
    public ResponseEntity<RsData<Void>> handleAuthorizationExceptions(Exception e) {
        if (e instanceof AuthenticationException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401", "로그인이 필요합니다.", null));
        }
        if (e instanceof AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RsData<>("403", "권한이 없습니다.", null));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RsData<>("403", "접근이 제한되었습니다.", null));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<RsData<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RsData<>("404", "존재하지 않는 사용자입니다.", null));
    }

    @ExceptionHandler({ LazyInitializationException.class, RuntimeException.class, Exception.class })
    public ResponseEntity<RsData<Void>> handleServerError(Exception e) {
        log.error("🔥 500 Internal Server Error", e);

        // ① 요청 메서드·경로
        String method = request.getMethod();
        String path   = request.getRequestURI();

        // ② 파라미터 JSON (body나 쿼리)
        String paramJson = extractParams(request); // 아래 유틸 참조

        alertNotifier.notifyError(
                "500 서버 내부 오류",
                e,
                method,
                path,
                paramJson
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RsData<>("500", "서버 오류가 발생했습니다.", null));
    }

    private String extractParams(HttpServletRequest req) {
        String contentType = req.getContentType() == null ? "" : req.getContentType();

        // 1) application/json
        if (contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            String json = (String) req.getAttribute("cachedRequestBody");
            return (json != null)
                    ? StringUtils.abbreviate(json, 300)
                    : "(빈 JSON 바디)";
        }

        // 2) GET 요청 → 쿼리스트링
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            String qs = req.getQueryString();
            return (qs != null && !qs.isBlank())
                    ? qs
                    : "(쿼리스트링 없음)";
        }

        // 3) 폼데이터 (x-www-form-urlencoded or multipart/form-data)
        Map<String, String[]> map = req.getParameterMap();
        if (!map.isEmpty()) {
            return map.entrySet().stream()
                    .map(e -> {
                        String key = e.getKey();
                        String[] vals = e.getValue();
                        return key + ":" + String.join(",", vals);
                    })
                    .collect(Collectors.joining("\n"));
        }

        return "(파라미터 없음)";
    }

    @ExceptionHandler(AccessTokenExpiredException.class)
    public ResponseEntity<RsData<Void>> handleAccessTokenExpiredException(AccessTokenExpiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new RsData<>("401-1", e.getMessage(), null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<RsData<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("X-Client-Base-Url".equals(ex.getHeaderName())) {
            return handleUnihubException(
                    new UnihubException("400", "X-Client-Base-Url 헤더가 필요합니다.")
            );
        }

        return handleUnihubException( new UnihubException("400", ex.getMessage()) );
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<RsData<Void>> handleAsyncNotUsable(AsyncRequestNotUsableException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RsData<>("503", "서비스를 일시적으로 사용할 수 없습니다.", null));
    }
}