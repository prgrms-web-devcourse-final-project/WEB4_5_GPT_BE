package com.WEB4_5_GPT_BE.unihub.global.exception;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.AccessTokenExpiredException;
import com.WEB4_5_GPT_BE.unihub.global.alert.AlertNotifier;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final AlertNotifier alertNotifier;

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
    alertNotifier.notifyError("500 서버 내부 오류", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RsData<>("500", "서버 오류가 발생했습니다.", null));
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
}
