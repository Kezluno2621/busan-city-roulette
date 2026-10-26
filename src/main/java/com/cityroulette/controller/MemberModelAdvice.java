package com.cityroulette.controller;

import com.cityroulette.dto.member.MemberProfile;
import com.cityroulette.service.MemberService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class MemberModelAdvice {
    private final MemberService memberService;

    public MemberModelAdvice(MemberService memberService) {
        this.memberService = memberService;
    }

    @ModelAttribute("currentMember")
    public MemberProfile currentMember(Principal principal) {
        return principal == null ? null : memberService.getProfile(principal.getName());
    }
}
