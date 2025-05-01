package com.WEB4_5_GPT_BE.unihub.domain.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class HomeController {


    @GetMapping
    public String home() throws UnknownHostException {

        InetAddress localHost = InetAddress.getLocalHost();
        return """
           Welcome to the Unihub API!!!!!
           hostAddress:%s
           """
                .formatted(localHost.getHostAddress());
    }

}
