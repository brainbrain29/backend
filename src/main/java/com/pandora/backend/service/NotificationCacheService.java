package com.pandora.backend.service;

import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通知缓存服务
 * 使用 Redis 缓存通知相关数据
 */
@Service
public class NotificationCacheService {

    @Autowired
    private RedisUtil redisUtil;

    // Redis Key 前缀
    private static final String UNREAD_COUNT_PREFIX = "unread_count:";
    private static final String RECENT_NOTICES_PREFIX = "recent_notices:";
    private static final String PENDING_NOTICES_PREFIX = "pending_notices:"; // 待推送通知队列

    /**
     * 增加未读通知数量
     */
    public void incrementUnreadCount(Integer userId) {
        String key = UNREAD_COUNT_PREFIX + userId;
        redisUtil.increment(key);
        // 设置过期时间 24 小时
        redisUtil.expire(key, 24, TimeUnit.HOURS);
    }

    /**
     * 设置未读通知数量（直接设置值）
     */
    public void setUnreadCount(Integer userId, long count) {
        String key = UNREAD_COUNT_PREFIX + userId;
        redisUtil.set(key, count, 24, TimeUnit.HOURS);
    }

    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(Integer userId) {
        String key = UNREAD_COUNT_PREFIX + userId;
        Object count = redisUtil.get(key);
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }

    /**
     * 清空未读通知数量
     */
    public void clearUnreadCount(Integer userId) {
        String key = UNREAD_COUNT_PREFIX + userId;
        redisUtil.delete(key);
    }

    /**
     * 减少未读通知数量
     */
    public void decrementUnreadCount(Integer userId) {
        String key = UNREAD_COUNT_PREFIX + userId;
        Long count = getUnreadCount(userId);
        if (count > 0) {
            redisUtil.decrement(key);
        }
    }

    /**
     * 缓存最近通知列表（最多缓存 10 条）
     */
    public void cacheRecentNotice(Integer userId, NoticeDTO notice) {
        String key = RECENT_NOTICES_PREFIX + userId;

        // 获取现有列表
        @SuppressWarnings("unchecked")
        List<NoticeDTO> notices = (List<NoticeDTO>) redisUtil.get(key);
        if (notices == null) {
            notices = new ArrayList<>();
        }

        // 添加新通知到列表头部
        notices.add(0, notice);

        // 只保留最近 10 条
        if (notices.size() > 10) {
            notices = notices.subList(0, 10);
        }

        // 缓存 5 分钟
        redisUtil.set(key, notices, 5, TimeUnit.MINUTES);
    }

    public void cacheRecentNotices(Integer userId, List<NoticeDTO> notices) {
        String key = RECENT_NOTICES_PREFIX + userId;

        // 只保留最近 10 条
        List<NoticeDTO> toCache = notices.size() > 10
                ? notices.subList(0, 10)
                : notices;
        // 一次性写入
        redisUtil.set(key, toCache, 5, TimeUnit.MINUTES);
    }

    /**
     * 获取缓存的最近通知列表
     */
    @SuppressWarnings("unchecked")
    public List<NoticeDTO> getRecentNotices(Integer userId) {
        String key = RECENT_NOTICES_PREFIX + userId;
        Object notices = redisUtil.get(key);
        return notices != null ? (List<NoticeDTO>) notices : new ArrayList<>();
    }

    /**
     * 清空用户的所有通知缓存
     */
    public void clearAllCache(Integer userId) {
        redisUtil.delete(UNREAD_COUNT_PREFIX + userId);
        redisUtil.delete(RECENT_NOTICES_PREFIX + userId);
    }

    // ==================== 待推送通知队列管理 ====================

    /**
     * 添加通知到待推送队列（用户离线时）
     * 使用 List 结构，保持通知顺序
     */
    public void addPendingNotice(Integer userId, NoticeDTO notice) {
        String key = PENDING_NOTICES_PREFIX + userId;
        
        // 获取现有队列
        @SuppressWarnings("unchecked")
        List<NoticeDTO> pendingList = (List<NoticeDTO>) redisUtil.get(key);
        if (pendingList == null) {
            pendingList = new ArrayList<>();
        }
        
        // 添加到队列尾部
        pendingList.add(notice);
        
        // 限制队列长度，最多保存 50 条
        if (pendingList.size() > 50) {
            pendingList = pendingList.subList(pendingList.size() - 50, pendingList.size());
        }
        
        // 缓存 7 天（用户可能长时间不上线）
        redisUtil.set(key, pendingList, 7, TimeUnit.DAYS);
        
        System.out.println("📥 通知已加入待推送队列，用户: " + userId + ", 队列长度: " + pendingList.size());
    }

    /**
     * 获取用户的所有待推送通知
     */
    @SuppressWarnings("unchecked")
    public List<NoticeDTO> getPendingNotices(Integer userId) {
        String key = PENDING_NOTICES_PREFIX + userId;
        Object notices = redisUtil.get(key);
        return notices != null ? (List<NoticeDTO>) notices : new ArrayList<>();
    }

    /**
     * 清空用户的待推送通知队列
     */
    public void clearPendingNotices(Integer userId) {
        String key = PENDING_NOTICES_PREFIX + userId;
        redisUtil.delete(key);
        System.out.println("🗑️ 已清空待推送队列，用户: " + userId);
    }

    /**
     * 获取待推送通知数量
     */
    public int getPendingNoticeCount(Integer userId) {
        List<NoticeDTO> notices = getPendingNotices(userId);
        return notices.size();
    }

    // ==================== 分布式锁管理 ====================

    /**
     * 尝试获取分布式锁
     */
    public Boolean tryLock(String lockKey, String lockValue, long timeout, TimeUnit unit) {
        return redisUtil.tryLock(lockKey, lockValue, timeout, unit);
    }

    /**
     * 释放分布式锁
     */
    public Boolean releaseLock(String lockKey, String lockValue) {
        return redisUtil.releaseLock(lockKey, lockValue);
    }
}