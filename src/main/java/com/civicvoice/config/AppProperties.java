package com.civicvoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Geo geo = new Geo();
    private RateLimit rateLimit = new RateLimit();
    private Cors cors = new Cors();
    private Sla sla = new Sla();
    private FileUpload file = new FileUpload();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 3_600_000L;
        private long refreshExpirationMs = 604_800_000L;
    }

    @Data
    public static class Geo {
        private double deduplicationRadiusMeters = 50.0;
        private double maxRadiusKm = 50.0;
    }

    @Data
    public static class RateLimit {
        private int issueSubmitPerHour = 10;
        private int authAttemptsPerMinute = 5;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }

    @Data
    public static class Sla {
        private int criticalHours = 24;
        private int highHours = 72;
        private int mediumHours = 168;
        private int lowHours = 720;
    }

    @Data
    public static class FileUpload {
        private String uploadDir = "./uploads";
        private int maxSizeMb = 10;
        private List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/webp", "video/mp4");
    }
}
