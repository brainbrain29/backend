package com.pandora.backend.controller;

import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.NotificationPushService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE 实时通知推送控制器
 * 用于建立服务器到客户端的实时连接
 */
@RestController
@RequestMapping("/notifications")
public class NotificationSseController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationPushService pushService;

    /**
     * 建立 SSE 连接
     * Flutter 前端调用此接口建立长连接
     * GET /notifications/stream
     * Header: Authorization: Bearer {token}
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(HttpServletRequest request) {
        // 从 JWT Filter 注入的属性获取 userId
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(401).build();
        }
        Integer userId = (Integer) uidObj;

        // 验证用户存在
        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(401).build();
        }

        // 创建 SSE 连接，超时 30 分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 注册连接到推送服务
        pushService.registerConnection(userId, emitter);

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return ResponseEntity.ok(emitter);
    }

    /**
     * 获取在线用户数量（用于监控）
     */
    @GetMapping("/online-count")
    public ResponseEntity<Map<String, Object>> getOnlineCount() {
        Map<String, Object> result = Map.of(
                "onlineCount", pushService.getOnlineCount(),
                "onlineUsers", pushService.getOnlineUserIds());
        return ResponseEntity.ok(result);
    }
}