package com.ankit.prguard.analysis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class AnalysisExecutorConfig {
    @Bean("analysisExecutor")
    ThreadPoolTaskExecutor analysisExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("pr-analysis-");
        executor.initialize();
        return executor;
    }
}
