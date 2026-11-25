package com.cityroulette.controller;

import com.cityroulette.dto.course.CourseGenerationRequest;
import com.cityroulette.dto.course.CourseGenerationResponse;
import com.cityroulette.service.CourseGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
