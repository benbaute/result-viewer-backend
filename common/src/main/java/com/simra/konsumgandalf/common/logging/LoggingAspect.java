package com.simra.konsumgandalf.common.logging;

import com.simra.konsumgandalf.common.constants.CronExpressions;
import com.simra.konsumgandalf.common.models.entities.MethodRun;
import com.simra.konsumgandalf.common.repositories.MethodRunRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

@Aspect
@Component
public class LoggingAspect {

	public static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);


	private final ConcurrentMap<String, Long> totalTimes = new ConcurrentHashMap<>();

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private MethodRunRepository methodRunRepository;

	@Around("@annotation(com.simra.konsumgandalf.common.logging.LogExecutionTime)")
	public Object methodTimeLogger(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) proceedingJoinPoint.getSignature();
        String key = sig.getDeclaringType().getSimpleName() + "->" + sig.getName();

        StopWatch stopWatch = new StopWatch(key);   // ✅ new each time
        stopWatch.start();

        try {
            return proceedingJoinPoint.proceed();
        } finally {
            stopWatch.stop();

            long elapsed = stopWatch.getTotalTimeMillis();

            methodRunRepository.save(new MethodRun(sig.getName(), elapsed));
            totalTimes.merge(key, elapsed, Long::sum);
        }
	}

	@Scheduled(cron = CronExpressions.EVERY_HOUR)
	public void printAllStopWatches() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		sb.append("Seconds       %       Task name\n");
		sb.append("------------------------------------------------------------------------\n");

		long totalTime = totalTimes.values().stream().mapToLong(Long::longValue).sum();

		totalTimes.keySet().stream().sorted().forEach(key -> {
			long time = totalTimes.get(key);
			double timeSeconds = time / 1000.0;
			int percentage = (int) ((time * 100.0) / totalTime);
			sb.append(String.format("%-13.4f %-8d %-30s\n", timeSeconds, percentage, key));
		});

		sb.append("------------------------------------------------------------------------\n");
		sb.append(String.format("Active Tasks: %d\n", taskExecutor.getActiveCount()));
		sb.append(String.format("Number of Queue items: %d\n", taskExecutor.getQueueSize()));
		sb.append("------------------------------------------------------------------------\n");

		if (logger.isInfoEnabled()) {
			logger.info(sb.toString());
		}
	}

}
