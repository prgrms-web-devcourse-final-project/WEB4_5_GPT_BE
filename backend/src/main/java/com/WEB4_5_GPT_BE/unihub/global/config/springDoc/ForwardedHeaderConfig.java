package com.WEB4_5_GPT_BE.unihub.global.config.springDoc;


import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * X-Forwarded-Proto 헤더를 읽어
 * 요청 스킴을 HTTPS로 변경하도록 설정합니다.
 * Swagger UI "Try it out"을 통한 api 호출 시
 * http로 호출하게 되는 mixed-content 오류를 방지합니다.
 */
@Configuration
public class ForwardedHeaderConfig {

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean =
                new FilterRegistrationBean<>(new ForwardedHeaderFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}