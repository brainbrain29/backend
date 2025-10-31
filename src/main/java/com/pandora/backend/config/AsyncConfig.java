package com.pandora.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 用于异步写入 Redis 缓存等非阻塞操作
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：常驻线程
        executor.setCorePoolSize(5);

        // 最大线程数：高峰期最多创建的线程
        executor.setMaxPoolSize(15);

        // 队列容量：等待队列大小
        executor.setQueueCapacity(100);

        // 线程名称前缀
        executor.setThreadNamePrefix("Async-Cache-");

        // 拒绝策略：队列满时，由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间：60秒无任务则回收
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
