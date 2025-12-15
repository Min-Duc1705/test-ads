package vn.project.magic_english.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache configuration for improved performance
 * Using Caffeine (in-memory cache) as fallback when Redis is not available
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager (in-memory)
     * Fast and lightweight, good for small to medium datasets
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "grammarChecks",
                "vocabularyStats",
                "homeStats",
                "userAchievements");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000) // Max 1000 entries per cache
                .expireAfterWrite(Duration.ofMinutes(10)) // Expire after 10 minutes
                .recordStats()); // Enable cache statistics

        return cacheManager;
    }
}
