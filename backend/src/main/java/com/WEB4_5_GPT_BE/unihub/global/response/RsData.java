package com.WEB4_5_GPT_BE.unihub.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsData<T> {
  private String code;
  private String message;
  private T data;

  public RsData(String code, String message) {
    this(code, message, (T) new Empty());
  }

  @JsonIgnore
  public int getStatusCode() {
    return Integer.parseInt(code);
  }
}
