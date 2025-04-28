package com.WEB4_5_GPT_BE.unihub.global.exception;

import lombok.Getter;

@Getter
public class UnihubException extends RuntimeException {
  private final String code;

  public UnihubException(String code, String message) {
    super(message);
    this.code = code;
  }

  public int getStatusCode() {
    return Integer.parseInt(code);
  }
}
