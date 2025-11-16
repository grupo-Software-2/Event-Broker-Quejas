package co.edu.uptc.broker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Value("${async.event-distribution.core-size:5}")
    private int eventDistCoreSize;

    @Value("${async.event-distribution.max-size:20}")
    private int eventDistMaxSize;

    @Value("${async.event-distribution.queue-capacity:500}")
    private int eventDistQueueCapacity;

    @Value("${async.subscriber-notification.core-size:10}")
    private int subNotifCoreSize;

    @Value("${async.subscriber-notification.max-size:50}")
    private int subNotifMaxSize;

    @Value("${async.subscriber-notification.queue-capacity:1000}")
    private int subNotifQueueCapacity;

    @Value("${async.event-processing.core-size:3}")
    private int eventProcCoreSize;

    @Value("${async.event-processing.max-size:10}")
    private int eventProcMaxSize;

    @Value("${async.event-processing.queue-capacity:200}")
    private int eventProcQueueCapacity;

    @Bean(name = "eventDistributionExecutor")
    public ThreadPoolTaskExecutor eventDistributionExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                "event-dist-",
                eventDistCoreSize,
                eventDistMaxSize,
                eventDistQueueCapacity
        );

        log.info("Created eventDistributionExecutor: core={}, max={}, queue={}",
                eventDistCoreSize, eventDistMaxSize, eventDistQueueCapacity);

        return executor;
    }

    @Bean(name = "subscriberNotificationExecutor")
    public ThreadPoolTaskExecutor subscriberNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                "subscriber-notify-",
                subNotifCoreSize,
                subNotifMaxSize,
                subNotifQueueCapacity
        );

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setAwaitTerminationSeconds(120);

        log.info("Created subscriberNotificationExecutor: core={}, max={}, queue={}",
                subNotifCoreSize, subNotifMaxSize, subNotifQueueCapacity);

        return executor;
    }

    @Bean(name = "eventProcessingExecutor")
    public ThreadPoolTaskExecutor eventProcessingExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                "event-proc-",
                eventProcCoreSize,
                eventProcMaxSize,
                eventProcQueueCapacity
        );

        log.info("Created eventProcessingExecutor: core={}, max={}, queue={}",
                eventProcCoreSize, eventProcMaxSize, eventProcQueueCapacity);

        return executor;
    }

    @Bean(name = "taskExecutor")
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                "async-task-",
                5,
                15,
                100
        );

        log.info("Created default taskExecutor");

        return executor;
    }

    private ThreadPoolTaskExecutor createExecutor(String threadNamePrefix,
                                                  int corePoolSize,
                                                  int maxPoolSize,
                                                  int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        executor.initialize();

        return executor;
    }
}
