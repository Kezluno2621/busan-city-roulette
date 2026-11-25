package com.cityroulette.domain.member.controller;

import com.cityroulette.domain.member.dto.MemberSignupRequest;
import com.cityroulette.domain.member.service.MemberService;
import com.cityroulette.domain.member.service.RegistrationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

/** 회원가입, 로그인 화면과 마이페이지를 제공합니다. */
@Controller
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new MemberSignupRequest());
        return "member/signup";
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
        return "member/signup";
    }

    @GetMapping("/login")
    public String login() {
        return "member/login";
    }

    @GetMapping("/my")
    public String myPage(Principal principal, Model model) {
        model.addAttribute("member", memberService.getProfile(principal.getName()));
        return "member/dashboard";
    }
}
