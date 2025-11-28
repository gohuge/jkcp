package kcp.core;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import kcp.threading.IMessageExecutor;
import kcp.threading.ITask;

import java.util.concurrent.TimeUnit;

/**
 * KCP调度任务
 *
 * <p>实现ITask、Runnable和TimerTask接口，负责KCP协议的定时调度操作。
 * 使用Netty的HashedWheelTimer实现高效的定时任务调度。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>协议更新</strong>：定期调用KCP协议的update方法</li>
 *   <li><strong>状态检查</strong>：检查连接状态和超时情况</li>
 *   <li><strong>定时刷新</strong>：触发数据包的定时发送</li>
 *   <li><strong>资源清理</strong>：处理超时连接的资源回收</li>
 * </ul>
 *
 * <p>调度机制：</p>
 * <ul>
 *   <li><strong>时间轮算法</strong>：使用HashedWheelTimer实现高效定时</li>
 *   <li><strong>动态调度</strong>：根据协议状态调整下次调度时间</li>
 *   <li><strong>精确控制</strong>：支持毫秒级的精确时间控制</li>
 * </ul>
 *
 * <p>执行模式：</p>
 * <pre>
 * 定时触发 → 提交到执行器 → 异步执行KCP更新 → 计算下次调度时间
 * </pre>
 *
 * <p>性能特点：</p>
 * <ul>
 *   <li><strong>高效率</strong>：时间轮算法O(1)的调度复杂度</li>
 *   <li><strong>低延迟</strong>：最小化调度延迟和抖动</li>
 *   <li><strong>可扩展</strong>：支持大量并发连接的调度</li>
 *   <li><strong>内存友好</strong>：合理使用内存，避免对象泄漏</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 创建调度任务
 * ScheduleTask scheduleTask = new ScheduleTask(
 *     messageExecutor,
 *     kcpConnection,
 *     timer
 * );
 *
 * // 调度首次执行
 * timer.newTimeout(scheduleTask, 10, TimeUnit.MILLISECONDS);
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>调度任务由框架自动管理，通常不需要手动创建</li>
 *   <li>确保调度任务的线程安全</li>
 *   <li>合理设置调度间隔，平衡性能和延迟</li>
 *   <li>处理异常情况，避免影响其他连接</li>
 * </ul>
 *
 * @since 1.0
 */
public class ScheduleTask implements ITask, Runnable, TimerTask {

    private final IMessageExecutor messageExecutor;

    private final Ukcp ukcp;

    private final HashedWheelTimer hashedWheelTimer;

    public ScheduleTask(IMessageExecutor messageExecutor, Ukcp ukcp, HashedWheelTimer hashedWheelTimer) {
        this.messageExecutor = messageExecutor;
        this.ukcp = ukcp;
        this.hashedWheelTimer = hashedWheelTimer;
    }

    //flush策略
    //1,在send调用后检查缓冲区如果可以发送直接调用update得到时间并存在ukcp内
    //2，定时任务到了检查ukcp的时间和自己的定时 如果可以发送则直接发送  时间延后则重新定时
    //定时任务发送成功后检测缓冲区  是否触发发送时间
    //3，读时间触发后检测检测缓冲区触发写事件
    //问题: 精准大量的flush触发会导致ack重复发送   流量增大？  不会的 ack只会发送一次
    @Override
    public void execute() {
        try {
            final Ukcp ukcp = this.ukcp;
            long now = System.currentTimeMillis();
            //判断连接是否关闭
            if (ukcp.getTimeoutMillis() != 0 && now - ukcp.getTimeoutMillis() > ukcp.getLastRecieveTime()) {
                ukcp.internalClose();
            }
            if (!ukcp.isActive()) {
                return;
            }
            long timeLeft = ukcp.getTsUpdate() - now;
            //判断执行时间是否到了
            if (timeLeft > 0) {
                hashedWheelTimer.newTimeout(this,timeLeft, TimeUnit.MILLISECONDS);
                return;
            }
            long next = ukcp.flush(now);
            hashedWheelTimer.newTimeout(this,next, TimeUnit.MILLISECONDS);
            //检测写缓冲区 如果能写则触发写事件
            if (!ukcp.getWriteBuffer().isEmpty() && ukcp.canSend(false))
            {
                ukcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.messageExecutor.execute(this);
    }

    @Override
    public void run(Timeout timeout) {
        run();
    }
}
