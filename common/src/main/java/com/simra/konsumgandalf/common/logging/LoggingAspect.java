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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Aspect
@Component
public class LoggingAspect {

	public static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // Reset after every print for completed tasks
	private final ConcurrentMap<String, Long> times = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> timesSubTasks = new ConcurrentHashMap<>();

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private MethodRunRepository methodRunRepository;

    public Object methodTimeLogger(ProceedingJoinPoint proceedingJoinPoint, ConcurrentMap<String, Long> totalTimesMap) throws Throwable {
        MethodSignature sig = (MethodSignature) proceedingJoinPoint.getSignature();
        String key = sig.getDeclaringType().getSimpleName() + "->" + sig.getName();

        StopWatch stopWatch = new StopWatch(key);
        stopWatch.start();

        try {
            return proceedingJoinPoint.proceed();
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();

            totalTimesMap.merge(key, elapsed, Long::sum);
        }
    }

	@Around("@annotation(com.simra.konsumgandalf.common.logging.LogExecutionTime)")
	public Object methodTimeLogger(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return methodTimeLogger(proceedingJoinPoint, times);
	}

    @Around("@annotation(com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask)")
    public Object methodTimeLoggerSubTask(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return methodTimeLogger(proceedingJoinPoint, timesSubTasks);
    }

	@Scheduled(cron = CronExpressions.EVERY_HOUR)
	public void printAllStopWatches() {
        List<MethodRun> methodRuns = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		sb.append("Seconds       %       Task name\n");
		sb.append("------------------------------------------------------------------------\n");

		long totalTime = times.values().stream().mapToLong(Long::longValue).sum();

		times.keySet().stream().sorted().forEach(key -> {
			long time = times.get(key);
            if (time <= 0) return;
            double timeSeconds = time / 1000.0;
            int percentage = (int) ((time * 100.0) / totalTime);
            sb.append(String.format("%-13.4f %-8d %-30s\n", timeSeconds, percentage, key));
            methodRuns.add(new MethodRun(key, time));
            times.put(key, 0L); // Reset timer
        });

        sb.append("------------------------------------------------------------------------\n");
        sb.append("Subtasks\n");
        sb.append("------------------------------------------------------------------------\n");
        sb.append("Seconds       Task name\n");
        sb.append("------------------------------------------------------------------------\n");
        timesSubTasks.keySet().stream().sorted().forEach(key -> {
            long time = timesSubTasks.get(key);
            if (time <= 0) return;
            double timeSeconds = time / 1000.0;
            sb.append(String.format("%-13.4f %-30s\n", timeSeconds, key));
            methodRuns.add(new MethodRun(key, time));
            timesSubTasks.put(key, 0L); // Reset timer
        });

		sb.append("------------------------------------------------------------------------\n");
		sb.append(String.format("Active Tasks: %d\n", taskExecutor.getActiveCount()));
		sb.append(String.format("Number of Queue items: %d\n", taskExecutor.getQueueSize()));
		sb.append("------------------------------------------------------------------------\n");

        if (!methodRuns.isEmpty()) {
            methodRunRepository.saveAll(methodRuns);
        }

		if (logger.isInfoEnabled()) {
			logger.info(sb.toString());
		}
	}

}
