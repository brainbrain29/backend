package com.pandora.backend.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        return result;
    }
}
