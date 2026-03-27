package com.loopers.infrastructure.pg;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class PgClientConfig {

    @Bean
    public Request.Options feignOptions() {
        // connectTimeout: 1s, readTimeout: 3s
        // PG 요청 지연이 최대 500ms이므로 여유있게 3s 설정
        return new Request.Options(1, TimeUnit.SECONDS, 3, TimeUnit.SECONDS, true);
    }
}
