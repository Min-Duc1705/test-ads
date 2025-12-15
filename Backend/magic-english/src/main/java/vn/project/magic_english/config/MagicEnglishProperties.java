package vn.project.magic_english.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "magic-english") // Standardized prefix
public class MagicEnglishProperties {

    private Ai ai = new Ai();

    @Data
    public static class Ai {
        private List<String> apiKeys;
    }
}
