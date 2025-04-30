package com.WEB4_5_GPT_BE.unihub.global.aspect;

import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {

  private final HttpServletResponse response;

  @Around(
      """
              (
                  within
                  (
                      @org.springframework.web.bind.annotation.RestController *
                  )
                  &&
                  (
                      @annotation(org.springframework.web.bind.annotation.GetMapping)
                      ||
                      @annotation(org.springframework.web.bind.annotation.PostMapping)
                      ||
                      @annotation(org.springframework.web.bind.annotation.PutMapping)
                      ||
                      @annotation(org.springframework.web.bind.annotation.DeleteMapping)
                      ||
                      @annotation(org.springframework.web.bind.annotation.PatchMapping)
                  )
              )
              ||
              @annotation(org.springframework.web.bind.annotation.ResponseBody)
              """)
  public Object responseAspect(ProceedingJoinPoint joinPoint) throws Throwable {
    Object result = joinPoint.proceed();

    if (result instanceof RsData rsData) {
      int statusCode = rsData.getStatusCode();
      response.setStatus(statusCode);
    }

    return result;
  }
}
