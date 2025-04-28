package com.WEB4_5_GPT_BE.unihub.global.exception;

import com.WEB4_5_GPT_BE.unihub.global.response.RsData;

public class UnihubException extends RuntimeException {

  private RsData<?> rsData;

  public UnihubException(String code, String message) {
    super(message);
    rsData = new RsData<>(code, message);
  }

  public String getCode() {
    return rsData.getCode();
  }

  public String getMessage() {
    return rsData.getMessage();
  }

  public int getStatusCode() {
    return rsData.getStatusCode();
  }
}
