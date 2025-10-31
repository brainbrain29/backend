package com.pandora.backend.controller;

import com.pandora.backend.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test/redis")
public class RedisTestController {

    @Autowired
    private RedisUtil redisUtil;

    @GetMapping("/set")
    public String testSet() {
        redisUtil.set("test_key", "Hello Redis!", 60, TimeUnit.SECONDS);
        return "设置成功";
    }

    @GetMapping("/get")
    public Map<String, Object> testGet() {
        Map<String, Object> result = new HashMap<>();
        Object value = redisUtil.get("test_key");

        result.put("key", "test_key");
        result.put("value", value);
        result.put("exists", redisUtil.hasKey("test_key"));
        result.put("message", value == null ? "键不存在或已过期，请先访问 /test/redis/set" : "获取成功");

        return result;
    }

    @GetMapping("/info")
    public Map<String, Object> testInfo() {
        Map<String, Object> info = new HashMap<>();

        // 测试字符串
        redisUtil.set("string_test", "测试字符串", 5, TimeUnit.MINUTES);
        info.put("string", redisUtil.get("string_test"));

        // 测试数字
        redisUtil.set("number_test", 100);
        info.put("number", redisUtil.get("number_test"));

        // 测试计数器
        redisUtil.set("counter", 0);
        redisUtil.increment("counter");
        redisUtil.increment("counter");
        info.put("counter", redisUtil.get("counter"));

        return info;
    }
}