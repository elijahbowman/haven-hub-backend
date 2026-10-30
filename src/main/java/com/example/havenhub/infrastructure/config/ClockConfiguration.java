package com.example.havenhub.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfiguration {

    // Injected wherever "today" matters, so tests can pin the date.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
