package com.kishanrao.shortener.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class ViewController {

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/auth");
    }

    @GetMapping("/shorten")
    public String shorten() {
        return "index";
    }

    @GetMapping("/stats/{code}")
    public String stats(@PathVariable String code) {
        return "stats";
    }

    @GetMapping({"/auth", "/login"})
    public String auth() {
        return "auth";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
