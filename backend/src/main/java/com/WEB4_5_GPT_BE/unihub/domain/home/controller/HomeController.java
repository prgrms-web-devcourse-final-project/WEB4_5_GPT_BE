package com.WEB4_5_GPT_BE.unihub.domain.home.controller;

import com.WEB4_5_GPT_BE.unihub.global.exception.home.HomeNoDataException;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping
    public String home() {
        return "Welcome to Unihub!";
    }

    @GetMapping("/join")
    public RsData<Void> join() {
        return new RsData<>(
            "201",
            "회원 가입이 완료되었습니다."
        );
    }

}
