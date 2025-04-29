package com.WEB4_5_GPT_BE.unihub.domain.Home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping
  public String home() {
    return "Welcome to the Unihub API!";
  }
}
