package com.w_s_backend.w_s.controllers;

import org.springframework.stereotype.Controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@AllArgsConstructor
public class HomeController {
    @GetMapping("/")
    public String homePage() {
        return "home";
    }
    
}
