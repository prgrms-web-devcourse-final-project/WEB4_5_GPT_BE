package com.WEB4_5_GPT_BE.unihub.global.exception;

import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnihubException.class)
    public ResponseEntity<RsData<Void>> ServiceExceptionHandle(UnihubException ex) {
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(
                new RsData<>(
                    ex.getCode(),
                    ex.getMessage()
                )
            );
    }

}
