package kcp.task;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * KCP任务调度器
 *
 * <p>负责KCP相关的定时任务调度，包括：
 * <ul>
 *   <li>定时更新KCP协议状态</li>
 *   <li>处理连接超时</li>
 *   <li>执行周期性检查</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class TaskScheduler {

    /**
     * 调度器名称
     */
    private final String name;

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService executor;

    /**
     * 是否已关闭
     */
    private volatile boolean shutdown = false;

    public TaskScheduler(String name, ScheduledExecutorService executor) {
        this.name = name;
        this.executor = executor;
    }

    /**
     * 调度任务
     *
     * @param task 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return 调度结果
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (shutdown) {
            throw new IllegalStateException("TaskScheduler is shutdown");
        }
        return executor.schedule(task, delay, unit);
    }

    /**
     * 调度固定速率任务
     *
     * @param task 要执行的任务
     * @param initialDelay 初始延迟时间
     * @param period 执行周期
     * @param unit 时间单位
     * @return 调度结果
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (shutdown) {
            throw new IllegalStateException("TaskScheduler is shutdown");
        }
        return executor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * 调度固定延迟任务
     *
     * @param task 要执行的任务
     * @param initialDelay 初始延迟时间
     * @param delay 任务间延迟
     * @param unit 时间单位
     * @return 调度结果
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (shutdown) {
            throw new IllegalStateException("TaskScheduler is shutdown");
        }
        return executor.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    /**
     * 立即执行任务
     *
     * @param task 要执行的任务
     */
    public void execute(Runnable task) {
        if (shutdown) {
            throw new IllegalStateException("TaskScheduler is shutdown");
        }
        executor.execute(task);
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
    }

    /**
     * 立即关闭调度器（强制）
     */
    public void shutdownNow() {
        shutdown = true;
        executor.shutdownNow();
    }

    /**
     * 等待关闭完成
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否成功关闭
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    /**
     * 是否已关闭
     */
    public boolean isShutdown() {
        return shutdown || executor.isShutdown();
    }

    /**
     * 是否已终止
     */
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    /**
     * 获取调度器名称
     */
    public String getName() {
        return name;
    }
}