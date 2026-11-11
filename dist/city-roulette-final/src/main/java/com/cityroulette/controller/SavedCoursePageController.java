package com.cityroulette.controller;

import com.cityroulette.dto.savedcourse.SavedCourseTitleUpdateRequest;
import com.cityroulette.service.SavedCourseService;
import com.cityroulette.service.SavedCourseConflictException;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/my/courses")
public class SavedCoursePageController {
    private final SavedCourseService courses;
    public SavedCoursePageController(SavedCourseService courses){this.courses=courses;}

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String title,
            Principal principal,Model model){model.addAttribute("courses",courses.list(principal.getName(),Math.max(0,page),12,title));model.addAttribute("titleQuery",title);return "my/courses";}
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,Principal principal,Model model){var course=courses.findOne(principal.getName(),id);model.addAttribute("course",course);model.addAttribute("titleUpdate",new SavedCourseTitleUpdateRequest(course.title(),course.version()));return "my/course-detail";}
    @PostMapping("/{id}/title")
    public String rename(@PathVariable Long id,@RequestParam String title,@RequestParam Long version,
            Principal principal,Model model,HttpServletResponse response){if(title==null||title.isBlank()||title.length()>100){var course=courses.findOne(principal.getName(),id);model.addAttribute("course",course);model.addAttribute("titleError","제목은 1~100자로 입력해주세요.");return "my/course-detail";}try{courses.rename(principal.getName(),id,new SavedCourseTitleUpdateRequest(title,version));return "redirect:/my/courses/"+id;}catch(SavedCourseConflictException exception){var course=courses.findOne(principal.getName(),id);model.addAttribute("course",course);model.addAttribute("titleError",exception.getMessage());response.setStatus(409);return "my/course-detail";}}
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,Principal principal){courses.delete(principal.getName(),id);return "redirect:/my/courses";}
}
