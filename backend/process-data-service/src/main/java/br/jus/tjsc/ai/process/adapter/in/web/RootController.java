package br.jus.tjsc.ai.process.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/swagger-ui.html";
    }
}
