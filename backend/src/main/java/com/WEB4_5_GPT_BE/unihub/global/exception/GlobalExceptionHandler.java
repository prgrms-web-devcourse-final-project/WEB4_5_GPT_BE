package com.WEB4_5_GPT_BE.unihub.global.exception;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.AccessTokenExpiredException;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UnihubException.class)
  public ResponseEntity<RsData<Void>> ServiceExceptionHandle(UnihubException e) {
    return ResponseEntity.status(e.getStatusCode()).body(new RsData<>(e.getCode(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RsData<Void>> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new RsData<>("400", message.isBlank() ? "잘못된 요청입니다." : message, null));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<RsData<Void>> handleAuthenticationException(AuthenticationException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new RsData<>("401", "로그인이 필요합니다.", null));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<RsData<Void>> handleAccessDeniedException(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new RsData<>("403", "권한이 없습니다.", null));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<RsData<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new RsData<>("404", "존재하지 않는 사용자입니다.", null));
  }

  @ExceptionHandler({ LazyInitializationException.class, RuntimeException.class, Exception.class })
  public ResponseEntity<RsData<Void>> handleServerError(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RsData<>("500", "서버 오류가 발생했습니다.", null));
  }

  @ExceptionHandler(AccessTokenExpiredException.class)
  public ResponseEntity<RsData<Void>> handleAccessTokenExpiredException(AccessTokenExpiredException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new RsData<>("401-1", e.getMessage(), null));
  }
}
