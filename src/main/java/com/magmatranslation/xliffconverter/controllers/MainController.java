package com.magmatranslation.xliffconverter.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MainController {
    
    @GetMapping("/")
    public Map<String, String> isWorking() {

        return Map.of("mensagem", "A API esta rodando!");
    }
    
}
