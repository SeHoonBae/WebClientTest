package com.webclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class WebClientTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebClientTestApplication.class, args);
    }

}
