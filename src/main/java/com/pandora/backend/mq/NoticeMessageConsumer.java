package com.pandora.backend.mq;

import com.pandora.backend.config.RabbitMQConfig;
import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.service.NotificationCacheService;
import com.pandora.backend.service.NotificationPushService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

/**
 * Consumer service for processing notification messages from RabbitMQ.
 * Handles Redis cache updates and SSE push to users.
 */
@Service
public class NoticeMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NoticeMessageConsumer.class);

    @Autowired
    private NotificationCacheService cacheService;

    @Autowired
    private NotificationPushService pushService;

    /**
     * Process notification messages from the queue.
     * - Updates Redis cache (unread count, recent notices)
     * - Pushes to user via SSE if online, or stores in pending queue if offline
     *
     * @param message     the notification message
     * @param channel     RabbitMQ channel for manual ack
     * @param deliveryTag message delivery tag for ack/nack
     */
    @RabbitListener(queues = RabbitMQConfig.NOTICE_PUSH_QUEUE, concurrency = "2-5")
    public void handleNotice(
            NoticeMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        String idempotentKey = "notice:processed:" + message.getNoticeId() + ":" + message.getReceiverId();

        try {
            // Idempotency check: prevent duplicate processing
            Boolean isNew = cacheService.trySetIdempotentKey(idempotentKey, Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                logger.info("⏭️ Message already processed, skipping. noticeId: {}", message.getNoticeId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            Integer receiverId = message.getReceiverId();
            NoticeDTO dto = toDTO(message);

            // 1. Update Redis cache
            cacheService.incrementUnreadCount(receiverId);
            cacheService.cacheRecentNotice(receiverId, dto);

            // 2. Push notification (online: SSE, offline: pending queue)
            pushService.pushNotification(receiverId, dto);

            // 3. Acknowledge successful processing
            channel.basicAck(deliveryTag, false);
            logger.info("✅ Notice processed successfully, noticeId: {}, receiverId: {}",
                    message.getNoticeId(), receiverId);

        } catch (Exception e) {
            logger.error("❌ Failed to process notice message: {}", e.getMessage(), e);
            try {
                // Reject and requeue the message for retry
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                logger.error("❌ Failed to nack message: {}", ioException.getMessage());
            }
        }
    }

    /**
     * Convert NoticeMessage to NoticeDTO for push/cache.
     */
    private NoticeDTO toDTO(NoticeMessage message) {
        NoticeDTO dto = new NoticeDTO();
        dto.setNoticeId(message.getNoticeId());
        dto.setContent(message.getContent());
        dto.setSenderName(message.getSenderName());
        dto.setCreatedTime(message.getCreatedTime());
        dto.setRelatedId(message.getRelatedId());
        dto.setTitle(message.getNoticeType());
        dto.setStatus(message.getStatus());
        return dto;
    }
}
