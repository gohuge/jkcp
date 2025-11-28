package kcp.kcp;

import kcp.fec.FecHandler;
import kcp.listener.KcpListener;
import kcp.threading.IMessageExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KCP连接包装类
 *
 * <p>包装了IKcpProtocol实例，提供连接级别的功能，包括：
 * <ul>
 *   <li>读写缓冲区管理</li>
 *   <li>消息处理器管理</li>
 *   <li>连接状态管理</li>
 *   <li>FEC支持</li>
 *   <li>超时处理</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class KcpConnection {

    /**
     * KCP协议实例
     */
    private final IKcpProtocol kcp;

    /**
     * 是否启用快速刷新
     */
    private boolean fastFlush = true;

    /**
     * 更新时间戳
     */
    private long tsUpdate = -1;

    /**
     * 连接是否活跃
     */
    private boolean active;

    /**
     * FEC编码器
     */
    private FecHandler fecEncode;

    /**
     * FEC解码器
     */
    private FecHandler fecDecode;

    /**
     * 写缓冲区
     */
    private final Queue<ByteBuf> writeBuffer;

    /**
     * 读缓冲区
     */
    private final Queue<ByteBuf> readBuffer;

    /**
     * 消息执行器
     */
    private final IMessageExecutor messageExecutor;

    /**
     * 连接监听器
     */
    private final KcpListener listener;

    /**
     * 超时时间（毫秒）
     */
    private final long timeoutMillis;

    /**
     * 通道管理器
     */
    private final Object channelManager;

    /**
     * 写处理状态
     */
    private final AtomicBoolean writeProcessing = new AtomicBoolean(false);

    /**
     * 读处理状态
     */
    private final AtomicBoolean readProcessing = new AtomicBoolean(false);

    /**
     * 读缓冲区增量计数器
     */
    private final AtomicInteger readBufferIncr = new AtomicInteger(-1);

    /**
     * 是否控制写缓冲区大小
     */
    private boolean controlWriteBufferSize = false;

    /**
     * 是否控制读缓冲区大小
     */
    private boolean controlReadBufferSize = false;

    /**
     * 写缓冲区最大大小
     */
    private int writeBufferSize = 0;

    /**
     * 读缓冲区最大大小
     */
    private int readBufferSize = 0;

    /**
     * 用户对象
     */
    private Object user;

    public KcpConnection(IKcpProtocol kcp, Queue<ByteBuf> writeBuffer, Queue<ByteBuf> readBuffer,
                        IMessageExecutor messageExecutor, KcpListener listener,
                        long timeoutMillis, Object channelManager) {
        this.kcp = kcp;
        this.writeBuffer = writeBuffer;
        this.readBuffer = readBuffer;
        this.messageExecutor = messageExecutor;
        this.listener = listener;
        this.timeoutMillis = timeoutMillis;
        this.channelManager = channelManager;
    }

    /**
     * 发送数据
     *
     * @param byteBuf 要发送的数据
     * @return 发送是否成功
     */
    public boolean write(ByteBuf byteBuf) {
        if (!active) {
            byteBuf.release();
            return false;
        }

        if (controlWriteBufferSize && writeBufferSize > 0) {
            int currentSize = writeBuffer.size();
            if (currentSize >= writeBufferSize) {
                // TODO: 这里做的不对 应该丢弃队列最早的那个消息包  这样子丢弃有一定的概率会卡死 以后优化
                byteBuf.release();
                return false;
            }
        }

        writeBuffer.offer(byteBuf);
        notifyWriteEvent();
        return true;
    }

    /**
     * 处理接收到的数据
     *
     * @param byteBuf 接收到的数据
     */
    public void handleReceive(ByteBuf byteBuf) {
        if (!active) {
            byteBuf.release();
            return;
        }

        if (controlReadBufferSize && readBufferSize > 0) {
            int currentSize = readBufferIncr.getAndUpdate(operand -> {
                if (operand == 0) {
                    return operand;
                }
                return --operand;
            });
            if (currentSize == 0) {
                // TODO: 这里做的不对 应该丢弃队列最早的那个消息包  这样子丢弃有一定的概率会卡死 以后优化
                byteBuf.release();
                return;
            }
        }

        readBuffer.offer(byteBuf);
        notifyReadEvent();
    }

    /**
     * 接收数据
     *
     * @param bufList 接收缓冲区列表
     * @return 接收的数据大小
     */
    public int recv(List<ByteBuf> bufList) {
        return kcp.recv(bufList);
    }

    /**
     * 发送数据
     *
     * @param byteBuf 要发送的数据
     * @return 发送的数据大小
     */
    public int send(ByteBuf byteBuf) {
        return kcp.send(byteBuf);
    }

    /**
     * 关闭连接
     */
    public void close() {
        active = false;

        // 清理写缓冲区
        ByteBuf buf;
        while ((buf = writeBuffer.poll()) != null) {
            buf.release();
        }

        // 清理读缓冲区
        while ((buf = readBuffer.poll()) != null) {
            buf.release();
        }

        // 释放FEC资源
        if (fecEncode != null) {
            fecEncode.release();
            fecEncode = null;
        }
        if (fecDecode != null) {
            fecDecode.release();
            fecDecode = null;
        }

        // 释放KCP协议实例
        kcp.release();

        // 通知监听器
        if (listener != null) {
            listener.handleClose(this);
        }
    }

    /**
     * 获取连接ID
     */
    public int getConv() {
        return kcp.getConv();
    }

    /**
     * 设置连接ID
     */
    public void setConv(int conv) {
        kcp.setConv(conv);
    }

    /**
     * 获取用户对象
     */
    public Object getUser() {
        return user != null ? user : kcp.getUser();
    }

    /**
     * 设置用户对象
     */
    public void setUser(Object user) {
        this.user = user;
        kcp.setUser(user);
    }

    /**
     * 获取KCP协议实例
     */
    public IKcpProtocol getKcp() {
        return kcp;
    }

    /**
     * 获取监听器
     */
    public KcpListener getKcpListener() {
        return listener;
    }

    /**
     * 获取消息执行器
     */
    public IMessageExecutor getMessageExecutor() {
        return messageExecutor;
    }

    /**
     * 是否活跃
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 设置活跃状态
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 获取超时时间
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * 是否启用快速刷新
     */
    public boolean isFastFlush() {
        return fastFlush;
    }

    /**
     * 设置快速刷新
     */
    public void setFastFlush(boolean fastFlush) {
        this.fastFlush = fastFlush;
    }

    /**
     * 设置FEC编码器
     */
    public void setFecEncode(FecHandler fecEncode) {
        this.fecEncode = fecEncode;
    }

    /**
     * 设置FEC解码器
     */
    public void setFecDecode(FecHandler fecDecode) {
        this.fecDecode = fecDecode;
    }

    /**
     * 设置写缓冲区控制
     */
    public void setWriteBufferControl(int size) {
        this.controlWriteBufferSize = size > 0;
        this.writeBufferSize = size;
    }

    /**
     * 设置读缓冲区控制
     */
    public void setReadBufferControl(int size) {
        this.controlReadBufferSize = size > 0;
        this.readBufferSize = size;
        this.readBufferIncr.set(size);
    }

    /**
     * 通知写事件
     */
    private void notifyWriteEvent() {
        if (writeProcessing.compareAndSet(false, true)) {
            // TODO: 实现写任务调度
        }
    }

    /**
     * 通知读事件
     */
    private void notifyReadEvent() {
        if (readProcessing.compareAndSet(false, true)) {
            // TODO: 实现读任务调度
        }
    }
}