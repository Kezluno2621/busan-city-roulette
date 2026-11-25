package com.cityroulette.domain.course.controller;

import com.cityroulette.domain.course.dto.SavedCourseResponse;
import com.cityroulette.domain.course.dto.SavedCourseTitleUpdateRequest;
import com.cityroulette.domain.course.service.SavedCourseConflictException;
import com.cityroulette.domain.course.service.SavedCourseService;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 회원의 저장 코스 목록과 상세 화면을 구성합니다. */
@Controller
@RequestMapping("/my/courses")
public class SavedCoursePageController {
    private static final String DETAIL_VIEW = "course/detail";
    private final SavedCourseService courses;

    public SavedCoursePageController(SavedCourseService courses) {
        this.courses = courses;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String title,
                       Principal principal, Model model) {
        model.addAttribute("courses", courses.list(principal.getName(), Math.max(0, page), 12, title));
        model.addAttribute("titleQuery", title);
        return "course/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        var course = loadDetail(id, principal, model);
        model.addAttribute("titleUpdate", new SavedCourseTitleUpdateRequest(course.title(), course.version()));
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/title")
    public String rename(@PathVariable Long id, @RequestParam String title, @RequestParam Long version,
                         Principal principal, Model model, HttpServletResponse response) {
        if (title == null || title.isBlank() || title.length() > 100) {
            loadDetail(id, principal, model);
            model.addAttribute("titleError", "제목은 1~100자로 입력해주세요.");
            return DETAIL_VIEW;
        }
        try {
            courses.rename(principal.getName(), id, new SavedCourseTitleUpdateRequest(title, version));
            return "redirect:/my/courses/" + id;
        } catch (SavedCourseConflictException exception) {
            loadDetail(id, principal, model);
            model.addAttribute("titleError", exception.getMessage());
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return DETAIL_VIEW;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        courses.delete(principal.getName(), id);
        return "redirect:/my/courses";
    }

    private SavedCourseResponse loadDetail(Long id, Principal principal, Model model) {
        SavedCourseResponse course = courses.findOne(principal.getName(), id);
        model.addAttribute("course", course);
        return course;
    }
}
