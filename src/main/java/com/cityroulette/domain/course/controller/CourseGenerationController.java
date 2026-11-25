package com.cityroulette.domain.course.controller;

import com.cityroulette.domain.course.dto.CourseGenerationRequest;
import com.cityroulette.domain.course.dto.CourseGenerationResponse;
import com.cityroulette.domain.course.service.CourseGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 코스 생성 POST 요청을 검증하고 Service에 넘깁니다. 생성 결과를 JSON으로 반환하며 저장은 별도 요청입니다.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseGenerationController {
    private final CourseGenerationService courses;

    public CourseGenerationController(CourseGenerationService courses) {
        this.courses = courses;
    }

    @PostMapping("/generate")
    public CourseGenerationResponse generate(@Valid @RequestBody CourseGenerationRequest request) {
        return courses.generate(request);
    }
}
