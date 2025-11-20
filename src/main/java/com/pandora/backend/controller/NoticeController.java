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
     * 标记单个通知为已读（前端查看通知详情时自动调用）
     * 将通知状态从 NOT_VIEWED → VIEWED
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

    /**
     * 批量确认收到通知（前端收到通知后调用）
     * 将通知状态从 NOT_RECEIVED → NOT_VIEWED
     * 
     * ========== 调用时机 ==========
     * 前端通过 SSE 收到通知后，延迟 500ms 批量确认
     * 
     * ========== 前端发送数据结构 ==========
     * POST /notices/batch-confirm-received
     * Headers:
     * Authorization: Bearer {token} ← userId 从 token 中解析
     * Content-Type: application/json
     * 
     * Body:
     * {
     * "noticeIds": [1, 2, 3, 4, 5] ← 前端收到的通知ID列表
     * }
     * 
     * ========== 后端返回数据结构 ==========
     * {
     * "success": true,
     * "confirmedCount": 5, ← 成功确认的数量
     * "confirmedNotices": [
     * {
     * "noticeId": 1,
     * "status": "未查看", ← 更新后的状态
     * "confirmedAt": "2025-11-18T12:17:00" ← 确认时间
     * },
     * ...
     * ]
     * }
     * 
     * ========== 处理逻辑 ==========
     * 1. 从 JWT Token 中获取 userId（不需要前端传递）
     * 2. 批量查询数据库中这些通知
     * 3. 筛选出状态为 NOT_RECEIVED 的通知
     * 4. 批量更新状态为 NOT_VIEWED
     * 5. 返回确认成功的通知列表
     */
    @PostMapping("/batch-confirm-received")
    public ResponseEntity<?> batchConfirmReceived(
            @RequestAttribute("userId") Integer userId,
            @RequestBody java.util.Map<String, java.util.List<Integer>> request) {

        // 验证用户身份（Token 解析失败会返回 null）
        if (userId == null) {
            return ResponseEntity.status(401).body("未授权：无效的 Token");
        }

        // 获取前端发送的通知ID列表
        java.util.List<Integer> noticeIds = request.get("noticeIds");
        if (noticeIds == null || noticeIds.isEmpty()) {
            return ResponseEntity.badRequest().body("noticeIds 不能为空");
        }

        System.out.println("📥 收到批量确认请求，userId: " + userId + ", 通知数量: " + noticeIds.size());

        try {
            // 调用 Service 批量确认
            // 参数1: userId（从 Token 获取，确保用户只能确认自己的通知）
            // 参数2: noticeIds（前端发送的通知ID列表）
            java.util.Map<String, Object> result = noticeService.batchConfirmReceived(userId, noticeIds);

            // 构建响应
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("confirmedCount", result.get("confirmedCount"));
            response.put("failedNoticeIds", result.get("failedNoticeIds"));

            System.out.println("✅ 批量确认成功，确认数量: " + result.get("confirmedCount"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ 批量确认失败: " + e.getMessage());

            java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 搜索通知（搜索内容和发送者姓名）
     * GET /notices/search?keyword={keyword}
     * 
     * @param userId  当前用户ID（从 JWT Token 中获取）
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchNotices(
            @RequestAttribute("userId") Integer userId,
            @RequestParam("keyword") String keyword) {

        if (userId == null) {
            return ResponseEntity.status(401).body("未授权：无效的 Token");
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("搜索关键词不能为空");
        }

        List<NoticeDTO> notices = noticeService.searchNotices(keyword, userId);
        return ResponseEntity.ok(notices);
    }
}
