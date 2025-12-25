package com.pandora.backend.mq;

import com.pandora.backend.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Producer service for sending notification messages to RabbitMQ.
 * Decouples notification creation from push/cache operations.
 */
@Service
public class NoticeMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(NoticeMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Send a notification message to the queue for async processing.
     * The consumer will handle Redis cache update and SSE push.
     *
     * @param message the notification message to send
     */
    public void sendNotice(NoticeMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTICE_EXCHANGE,
                    RabbitMQConfig.NOTICE_PUSH_ROUTING_KEY,
                    message);
            logger.info("📤 Notice message sent to queue, noticeId: {}, receiverId: {}",
                    message.getNoticeId(), message.getReceiverId());
        } catch (Exception e) {
            logger.error("❌ Failed to send notice message to queue: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification to queue", e);
        }
    }
}
