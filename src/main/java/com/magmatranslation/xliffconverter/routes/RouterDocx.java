package com.magmatranslation.xliffconverter.routes;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RouterDocx {

    @GetMapping("/docx/get-translated-docx")
    public Map<String, String> uhbub() {
        return Map.of("mensagem", "O docx sera traduzido");
    }
}
