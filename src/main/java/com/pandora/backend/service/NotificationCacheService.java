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
}