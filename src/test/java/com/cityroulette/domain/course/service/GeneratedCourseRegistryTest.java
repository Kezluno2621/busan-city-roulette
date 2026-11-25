package com.cityroulette.domain.course.service;

import com.cityroulette.domain.course.dto.CourseGenerationRequest;
import com.cityroulette.domain.course.dto.CourseGenerationResponse;
import com.cityroulette.domain.course.dto.CourseOriginDto;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GeneratedCourseRegistryTest {
    @Test void rejectsTokenAfterThirtyMinuteTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-29T00:00:00Z"));
        GeneratedCourseRegistry registry = new GeneratedCourseRegistry(clock);
        var request = new CourseGenerationRequest(null,35.1,129.1,List.of(),null,180,30000L,50,1,42L);
        var response = new CourseGenerationResponse("fixture",new CourseOriginDto(null,"출발",35.1,129.1),
                clock.instant(),1,1,List.of(),false,"fixture",List.of(),null);
        String token = registry.issue(request,response);
        clock.advance(Duration.ofMinutes(31));
        assertThatThrownBy(() -> registry.consume(token)).isInstanceOf(InvalidGeneratedCourseException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant){this.instant=instant;}
        void advance(Duration duration){instant=instant.plus(duration);}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return this;}
        @Override public Instant instant(){return instant;}
    }
}
