package com.pandora.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.dto.NoticeStatusDTO;
import com.pandora.backend.service.NoticeService;

@RestController
@RequestMapping("/notices")
public class NoticeController {
    @Autowired
    private NoticeService noticeService;

    // TODO:需要先返回通知摘要,再返回通知详情吗？
    /**
     * 获取当前用户的未读通知
     * 安全：从 JWT Token 中获取 userId，防止越权访问
     */
    @GetMapping("/me/unread")
    public ResponseEntity<List<NoticeDTO>> getUnreadNotice(@RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<NoticeDTO> notices = noticeService.getUnreadNotice(userId);
        return ResponseEntity.ok(notices);
    }

    /**
     * 获取当前用户的所有通知
     * 安全：从 JWT Token 中获取 userId，防止越权访问
     */
    @GetMapping("/me/all")
    public ResponseEntity<List<NoticeDTO>> getAllNotice(@RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<NoticeDTO> notices = noticeService.getAllNotice(userId);
        return ResponseEntity.ok(notices);
    }

    /**
     * 检查当前用户的未读通知数量
     * 安全：从 JWT Token 中获取 userId，防止越权访问
     */
    @GetMapping("/check")
    public ResponseEntity<NoticeStatusDTO> checkNotice(@RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        NoticeStatusDTO status = noticeService.checkUnreadNotice(userId);
        return ResponseEntity.ok(status);
    }

    /**
     * 标记单个通知为已读
     */
    @PutMapping("/mark-read/{noticeId}")
    public ResponseEntity<Void> markAsRead(
            @RequestAttribute("userId") Integer userId,
            @PathVariable Integer noticeId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        noticeService.markAsRead(userId, noticeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        noticeService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @RequestAttribute("userId") Integer userId,
            @PathVariable Integer noticeId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        noticeService.deleteNotice(userId, noticeId);
        return ResponseEntity.ok().build();
    }
}
