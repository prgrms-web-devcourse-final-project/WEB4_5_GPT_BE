package com.WEB4_5_GPT_BE.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

  @GetMapping
  public String home() {
    return "Welcome to Unihub!";
  }
}
