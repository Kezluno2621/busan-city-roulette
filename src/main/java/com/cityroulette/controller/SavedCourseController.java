package com.cityroulette.controller;

import com.cityroulette.dto.savedcourse.*;
import com.cityroulette.service.SavedCourseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 회원의 저장 코스를 등록·조회·수정·삭제합니다. */
@RestController
@RequestMapping("/api/courses")
@Validated
public class SavedCourseController {
    private final SavedCourseService courses;
    public SavedCourseController(SavedCourseService courses) { this.courses = courses; }

    @PostMapping
    public ResponseEntity<SavedCourseResponse> save(@Valid @RequestBody SavedCourseCreateRequest request,
            Principal principal) {
        SavedCourseResponse saved = courses.save(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/courses/" + saved.id())).body(saved);
    }

    @GetMapping
    public SavedCoursePageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "") String title, Principal principal) {
        return courses.list(principal.getName(), page, size, title);
    }

    @GetMapping("/{id}")
    public SavedCourseResponse detail(@PathVariable Long id, Principal principal) {
        return courses.findOne(principal.getName(), id);
    }

    @PatchMapping("/{id}")
    public SavedCourseResponse rename(@PathVariable Long id,
            @Valid @RequestBody SavedCourseTitleUpdateRequest request, Principal principal) {
        return courses.rename(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        courses.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
