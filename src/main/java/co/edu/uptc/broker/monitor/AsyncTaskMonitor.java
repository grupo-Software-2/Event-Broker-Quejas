package co.edu.uptc.broker.monitor;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class AsyncTaskMonitor {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskMonitor.class);

    @Autowired
    private ApplicationContext applicationContext;

    private Map<String, ThreadPoolTaskExecutor> executors = new HashMap<>();

    @PostConstruct
    public void initializeMonitor() {
        log.info("Initializing AsyncTaskMonitor...");

        String[] executorNames = {
                "eventDistributionExecutor",
                "eventProcessingExecutor",
                "subscriberNotificationExecutor"
        };

        for (String name : executorNames) {
            try {
                Object bean = applicationContext.getBean(name);
                if (bean instanceof ThreadPoolTaskExecutor) {
                    executors.put(name, (ThreadPoolTaskExecutor) bean);
                    log.info("✓ Found executor: {}", name);
                } else {
                    log.warn("✗ Bean {} is not a ThreadPoolTaskExecutor", name);
                }
            } catch (Exception e) {
                log.warn("✗ Executor {} not found: {}", name, e.getMessage());
            }
        }

        log.info("AsyncTaskMonitor initialized with {} executors", executors.size());
    }

    @Scheduled(fixedRate = 300000) // 5 minutos
    public void monitorThreadPools() {
        if (executors.isEmpty()) {
            log.warn("No executors available for monitoring");
            return;
        }

        log.info("=== Async Task Execution Monitor ===");

        executors.forEach((name, executor) -> {
            try {
                logPoolStats(name, executor);
            } catch (Exception e) {
                log.error("Error monitoring {}: {}", name, e.getMessage());
            }
        });

        log.info("=====================================");
    }

    @Scheduled(fixedRate = 60000) // 1 minuto
    public void checkPoolSaturation() {
        executors.forEach((name, executor) -> {
            try {
                checkSaturation(name, executor, 0.8);
            } catch (Exception e) {
                log.error("Error checking saturation for {}: {}", name, e.getMessage());
            }
        });
    }

    private void logPoolStats(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();

        int activeCount = threadPool.getActiveCount();
        int poolSize = threadPool.getPoolSize();
        int corePoolSize = threadPool.getCorePoolSize();
        int maxPoolSize = threadPool.getMaximumPoolSize();
        long completedTasks = threadPool.getCompletedTaskCount();
        long totalTasks = threadPool.getTaskCount();
        int queueSize = threadPool.getQueue().size();

        log.info("{} - Active: {}/{}, Pool: {}/{}, Queue: {}, Completed: {}/{}",
                poolName, activeCount, maxPoolSize, poolSize, maxPoolSize,
                queueSize, completedTasks, totalTasks);
    }

    private void checkSaturation(String poolName, ThreadPoolTaskExecutor executor, double threshold) {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();

        int activeCount = threadPool.getActiveCount();
        int maxPoolSize = threadPool.getMaximumPoolSize();
        int queueSize = threadPool.getQueue().size();
        int queueCapacity = executor.getQueueCapacity();

        double poolUtilization = (double) activeCount / maxPoolSize;
        double queueUtilization = queueCapacity > 0 ? (double) queueSize / queueCapacity : 0;

        if (poolUtilization >= threshold) {
            log.warn("⚠️ {} Pool is {}% saturated! Consider scaling up.",
                    poolName, (int) (poolUtilization * 100));
        }

        if (queueCapacity > 0 && queueUtilization >= threshold) {
            log.warn("⚠️ {} Queue is {}% full! Tasks may be rejected soon.",
                    poolName, (int) (queueUtilization * 100));
        }
    }

    public ExecutorMetrics getMetrics() {
        Map<String, PoolMetrics> metrics = new HashMap<>();

        executors.forEach((name, executor) -> {
            try {
                metrics.put(name, getPoolMetrics(name, executor));
            } catch (Exception e) {
                log.error("Error getting metrics for {}: {}", name, e.getMessage());
            }
        });

        return new ExecutorMetrics(metrics);
    }

    private PoolMetrics getPoolMetrics(String name, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();

        return new PoolMetrics(
                name,
                threadPool.getActiveCount(),
                threadPool.getPoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getQueue().size(),
                executor.getQueueCapacity(),
                threadPool.getCompletedTaskCount(),
                threadPool.getTaskCount()
        );
    }

    // DTOs para métricas
    public static class ExecutorMetrics {
        private final Map<String, PoolMetrics> pools;

        public ExecutorMetrics(Map<String, PoolMetrics> pools) {
            this.pools = pools;
        }

        public Map<String, PoolMetrics> getPools() {
            return pools;
        }

        public PoolMetrics getEventDistribution() {
            return pools.get("eventDistributionExecutor");
        }

        public PoolMetrics getEventProcessing() {
            return pools.get("eventProcessingExecutor");
        }

        public PoolMetrics getSubscriberNotification() {
            return pools.get("subscriberNotificationExecutor");
        }
    }

    public static class PoolMetrics {
        private final String name;
        private final int activeThreads;
        private final int poolSize;
        private final int maxPoolSize;
        private final int queueSize;
        private final int queueCapacity;
        private final long completedTasks;
        private final long totalTasks;
        private final double poolUtilization;
        private final double queueUtilization;

        public PoolMetrics(String name, int activeThreads, int poolSize, int maxPoolSize,
                           int queueSize, int queueCapacity, long completedTasks, long totalTasks) {
            this.name = name;
            this.activeThreads = activeThreads;
            this.poolSize = poolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueSize = queueSize;
            this.queueCapacity = queueCapacity;
            this.completedTasks = completedTasks;
            this.totalTasks = totalTasks;
            this.poolUtilization = maxPoolSize > 0 ? (double) activeThreads / maxPoolSize : 0;
            this.queueUtilization = queueCapacity > 0 ? (double) queueSize / queueCapacity : 0;
        }

        // Getters
        public String getName() {
            return name;
        }

        public int getActiveThreads() {
            return activeThreads;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public long getCompletedTasks() {
            return completedTasks;
        }

        public long getTotalTasks() {
            return totalTasks;
        }

        public double getPoolUtilization() {
            return poolUtilization;
        }

        public double getQueueUtilization() {
            return queueUtilization;
        }
    }
}
