package com.WEB4_5_GPT_BE.unihub.global.exception;

import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import lombok.Getter;

@Getter
public class UnihubException extends RuntimeException {

  private RsData<Void> rsData;

  public UnihubException(String code, String message) {
    super(message);
    rsData = new RsData<>(code, message);
  }

  public int getStatusCode() {
    return rsData.getStatusCode();
  }
}
