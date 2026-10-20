package com.fiap.WtcSync.infrastructure.configs;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
public class ChatPushConfig {
    @Bean("chatPushExecutor")
    public ThreadPoolTaskExecutor chatPushExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); executor.setMaxPoolSize(2); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("chat-push-");
        return executor;
    }
}
