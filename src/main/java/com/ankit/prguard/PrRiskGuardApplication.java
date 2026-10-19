package com.ankit.prguard;

import com.ankit.prguard.shared.PrGuardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties(PrGuardProperties.class)
public class PrRiskGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrRiskGuardApplication.class, args);
    }
}
