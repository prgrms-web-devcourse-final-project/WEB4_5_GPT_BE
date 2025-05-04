package com.WEB4_5_GPT_BE.unihub;

import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@RedisTestContainerConfig
class UnihubApplicationTests {

  @Test
  void contextLoads() {}
}
