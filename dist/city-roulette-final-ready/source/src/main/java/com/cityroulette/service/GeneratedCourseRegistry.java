package com.cityroulette.service;

import com.cityroulette.dto.course.CourseGenerationRequest;
import com.cityroulette.dto.course.CourseGenerationResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class GeneratedCourseRegistry {
    private static final Duration TTL = Duration.ofMinutes(30);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public GeneratedCourseRegistry() { this(Clock.systemUTC()); }
    GeneratedCourseRegistry(Clock clock) { this.clock = clock; }

    public String issue(CourseGenerationRequest request, CourseGenerationResponse response) {
        cleanup();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entries.put(token, new Entry(new GeneratedCourseSnapshot(request, response), Instant.now(clock).plus(TTL)));
        return token;
    }

    public GeneratedCourseSnapshot consume(String token) {
        if (token == null || token.isBlank()) throw new InvalidGeneratedCourseException();
        Entry entry = entries.remove(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now(clock))) throw new InvalidGeneratedCourseException();
        return entry.snapshot();
    }

    private void cleanup() {
        Instant now = Instant.now(clock);
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record GeneratedCourseSnapshot(CourseGenerationRequest request, CourseGenerationResponse response) { }
    private record Entry(GeneratedCourseSnapshot snapshot, Instant expiresAt) { }
}
