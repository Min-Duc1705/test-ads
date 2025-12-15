package vn.project.magic_english.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async configuration for background tasks
 * Improves API response time by processing non-critical tasks asynchronously
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for async tasks
     * - Achievement checks
     * - Statistics updates
     * - Email notifications (future)
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Minimum threads
        executor.setMaxPoolSize(10); // Maximum threads
        executor.setQueueCapacity(100); // Queue size
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
