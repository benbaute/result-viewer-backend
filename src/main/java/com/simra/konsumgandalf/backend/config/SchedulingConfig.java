package com.simra.konsumgandalf.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

	private static final Logger _logger = LoggerFactory.getLogger(SchedulingConfig.class);

	private final int CORE_POOL_SIZE;

	public SchedulingConfig(@Value("${SCHEDULING_CORE_POOL_SIZE}") int corePoolSize) {
		this.CORE_POOL_SIZE = corePoolSize;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(CORE_POOL_SIZE);
		scheduler.setThreadNamePrefix("scheduled-task-");
		scheduler.initialize();
		taskRegistrar.setTaskScheduler(scheduler);
	}

}
