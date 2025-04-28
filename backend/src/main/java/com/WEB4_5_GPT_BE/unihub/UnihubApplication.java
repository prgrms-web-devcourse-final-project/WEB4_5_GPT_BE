package com.WEB4_5_GPT_BE.unihub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class UnihubApplication {

  public static void main(String[] args) {
    SpringApplication.run(UnihubApplication.class, args);
  }
}
