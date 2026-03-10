package com.kishanrao.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SwiftLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwiftLinkApplication.class, args);
    }
}
