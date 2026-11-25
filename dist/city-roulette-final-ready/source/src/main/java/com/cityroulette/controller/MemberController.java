package com.cityroulette.controller;

import com.cityroulette.dto.member.MemberSignupRequest;
import com.cityroulette.service.MemberService;
import com.cityroulette.service.RegistrationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new MemberSignupRequest());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupRequest") MemberSignupRequest request,
                         BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            try {
                memberService.register(request);
                return "redirect:/login";
            } catch (RegistrationException exception) {
                if (exception.getField() == null) {
                    bindingResult.reject("registration", exception.getMessage());
                } else {
                    bindingResult.rejectValue(exception.getField(), "registration", exception.getMessage());
                }
            }
        }
        request.setPassword(null);
        request.setPasswordConfirm(null);
        // password 타입 th:field는 거부된 입력값도 HTML value로 재표시하지 않습니다.
        return "signup";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/my")
    public String myPage(Principal principal, Model model) {
        model.addAttribute("member", memberService.getProfile(principal.getName()));
        return "my/index";
    }
}
