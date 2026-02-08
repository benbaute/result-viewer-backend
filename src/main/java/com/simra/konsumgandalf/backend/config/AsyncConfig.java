package com.simra.konsumgandalf.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	private static final Logger _logger = LoggerFactory.getLogger(AsyncConfig.class);

	private final int CORE_POOL_SIZE;

	private final int MAX_POOL_SIZE;

	private final int QUEUE_CAPACITY;

	public AsyncConfig(@Value("${ASYNC_CORE_POOL_SIZE}") int corePoolSize,
			@Value("${ASYNC_MAX_POOL_SIZE}") int maxPoolSize, @Value("${ASYNC_QUEUE_CAPACITY}") int queueCapacity) {
		this.CORE_POOL_SIZE = corePoolSize;
		this.MAX_POOL_SIZE = maxPoolSize;
		this.QUEUE_CAPACITY = queueCapacity;
	}

	@Bean(name = "taskExecutor")
	public ThreadPoolTaskExecutor taskExecutor() {
		_logger.info("Creating Background Async Task Executor");
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(CORE_POOL_SIZE);
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setQueueCapacity(QUEUE_CAPACITY);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setThreadNamePrefix("Async-");
		executor.initialize();
		return executor;
	}

}
