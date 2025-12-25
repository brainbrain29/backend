package com.pandora.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 通知模块配置类。
 * 使用 Direct Exchange（直连交换机）实现通知消息的精确路由。
 *
 * <p>
 * 核心组件说明：
 * </p>
 * <ul>
 * <li>MessageConverter - 消息转换器，将 Java 对象序列化为 JSON</li>
 * <li>RabbitTemplate - 发送消息的模板类，类似 JdbcTemplate</li>
 * <li>DirectExchange - 直连交换机，根据 routing key 精确匹配路由</li>
 * <li>Queue - 消息队列，存储待消费的消息</li>
 * <li>Binding - 绑定关系，将队列绑定到交换机并指定 routing key</li>
 * </ul>
 *
 * <p>
 * 官方文档：
 * </p>
 * <ul>
 * <li>Spring AMQP: https://docs.spring.io/spring-amqp/reference/</li>
 * <li>RabbitMQ: https://www.rabbitmq.com/documentation.html</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 常量定义 ====================

    /**
     * 交换机名称。
     * 交换机是消息的"路由器"，接收生产者发送的消息，根据规则转发到队列。
     */
    public static final String NOTICE_EXCHANGE = "notice.exchange";

    /**
     * 通知推送队列名称。
     * 队列是消息的"缓冲区"，存储等待消费者处理的消息。
     */
    public static final String NOTICE_PUSH_QUEUE = "notice.push.queue";

    /**
     * 路由键。
     * 生产者发送消息时指定 routing key，交换机根据它决定消息发往哪个队列。
     * Direct Exchange 要求 routing key 完全匹配。
     */
    public static final String NOTICE_PUSH_ROUTING_KEY = "notice.push";

    /**
     * 死信交换机名称（Dead Letter Exchange）。
     * 当消息被拒绝、过期或队列满时，消息会被转发到死信交换机。
     */
    public static final String NOTICE_DLX = "notice.dlx";

    /**
     * 死信队列名称（Dead Letter Queue）。
     * 存储"死亡"的消息，便于后续排查问题或重新处理。
     */
    public static final String NOTICE_DLQ = "notice.dlq";

    // ==================== Bean 定义 ====================

    /**
     * 消息转换器：将 Java 对象转换为 JSON 格式。
     *
     * <p>
     * 作用：
     * </p>
     * <ul>
     * <li>发送时：NoticeMessage 对象 → JSON 字符串</li>
     * <li>接收时：JSON 字符串 → NoticeMessage 对象</li>
     * </ul>
     *
     * @return Jackson2JsonMessageConverter 实例
     * @see org.springframework.amqp.support.converter.MessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitMQ 操作模板，用于发送消息。
     *
     * <p>
     * 类似于：
     * </p>
     * <ul>
     * <li>JdbcTemplate - 操作数据库</li>
     * <li>RedisTemplate - 操作 Redis</li>
     * <li>RestTemplate - 发送 HTTP 请求</li>
     * </ul>
     *
     * @param connectionFactory RabbitMQ 连接工厂（Spring Boot 自动配置）
     * @return 配置好 JSON 转换器的 RabbitTemplate
     * @see org.springframework.amqp.rabbit.core.RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 直连交换机：根据 routing key 精确匹配路由消息。
     *
     * <p>
     * 参数说明：
     * </p>
     * <ul>
     * <li>name - 交换机名称</li>
     * <li>durable (true) - 持久化，RabbitMQ 重启后交换机仍存在</li>
     * <li>autoDelete (false) - 不自动删除，即使没有队列绑定</li>
     * </ul>
     *
     * @return DirectExchange 实例
     * @see org.springframework.amqp.core.DirectExchange
     */
    @Bean
    public DirectExchange noticeExchange() {
        return new DirectExchange(NOTICE_EXCHANGE, true, false);
    }

    /**
     * 通知推送队列。
     *
     * <p>
     * 队列属性：
     * </p>
     * <ul>
     * <li>durable - 持久化，RabbitMQ 重启后队列和消息仍存在</li>
     * <li>x-message-ttl - 消息过期时间（24小时），过期后进入死信队列</li>
     * <li>x-dead-letter-exchange - 死信交换机，处理失败/过期的消息</li>
     * <li>x-dead-letter-routing-key - 死信路由键</li>
     * </ul>
     *
     * @return Queue 实例
     * @see org.springframework.amqp.core.Queue
     */
    @Bean
    public Queue noticePushQueue() {
        return QueueBuilder
                .durable(NOTICE_PUSH_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 小时
                .withArgument("x-dead-letter-exchange", NOTICE_DLX)
                .withArgument("x-dead-letter-routing-key", "notice.dead")
                .build();
    }

    /**
     * 绑定关系：将队列绑定到交换机，并指定路由键。
     *
     * <p>
     * 绑定后的路由规则：
     * </p>
     * 
     * <pre>
     * 生产者发送消息:
     *   exchange = "notice.exchange"
     *   routingKey = "notice.push"
     *        ↓
     * 交换机匹配 routingKey
     *        ↓
     * 消息投递到 "notice.push.queue"
     * </pre>
     *
     * @return Binding 实例
     * @see org.springframework.amqp.core.Binding
     */
    @Bean
    public Binding noticePushBinding() {
        return BindingBuilder
                .bind(noticePushQueue())
                .to(noticeExchange())
                .with(NOTICE_PUSH_ROUTING_KEY);
    }

    /**
     * 死信交换机：接收"死亡"的消息。
     *
     * <p>
     * 消息进入死信队列的情况：
     * </p>
     * <ul>
     * <li>消息被消费者拒绝（basicNack/basicReject）且不重新入队</li>
     * <li>消息过期（超过 TTL）</li>
     * <li>队列达到最大长度</li>
     * </ul>
     *
     * @return DirectExchange 实例
     */
    @Bean
    public DirectExchange noticeDlx() {
        return new DirectExchange(NOTICE_DLX, true, false);
    }

    /**
     * 死信队列：存储失败/过期的消息。
     *
     * <p>
     * 用途：
     * </p>
     * <ul>
     * <li>排查问题：查看哪些消息处理失败</li>
     * <li>重新处理：将死信消息重新发送到正常队列</li>
     * <li>监控告警：死信队列消息堆积时触发告警</li>
     * </ul>
     *
     * @return Queue 实例
     */
    @Bean
    public Queue noticeDlq() {
        return QueueBuilder.durable(NOTICE_DLQ).build();
    }

    /**
     * 死信队列绑定：将死信队列绑定到死信交换机。
     *
     * @return Binding 实例
     */
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(noticeDlq())
                .to(noticeDlx())
                .with("notice.dead");
    }
}
