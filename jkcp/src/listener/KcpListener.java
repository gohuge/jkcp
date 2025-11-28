package kcp.listener;

import io.netty.buffer.ByteBuf;
import kcp.kcp.KcpConnection;

/**
 * KCP连接事件监听器接口
 *
 * <p>定义了KCP连接生命周期中的各种事件回调方法，
 * 包括连接建立、数据接收、异常处理和连接关闭等。</p>
 *
 * @since 1.0
 */
public interface KcpListener {

    /**
     * 连接建立后的回调
     *
     * <p>当KCP连接成功建立时调用此方法。</p>
     *
     * @param connection KCP连接实例
     */
    void onConnected(KcpConnection connection);

    /**
     * 接收到数据的回调
     *
     * <p>当接收到数据时调用此方法，数据已经经过KCP协议处理。</p>
     *
     * @param data 接收到的数据缓冲区
     * @param connection KCP连接实例
     */
    void handleReceive(ByteBuf data, KcpConnection connection);

    /**
     * 处理异常的回调
     *
     * <p>当发生异常时调用此方法，调用后该KCP连接将被关闭。</p>
     *
     * @param ex 异常信息
     * @param connection 发生异常的KCP连接，null表示非KCP错误
     */
    void handleException(Throwable ex, KcpConnection connection);

    /**
     * 连接关闭的回调
     *
     * <p>当KCP连接关闭时调用此方法。</p>
     *
     * @param connection 关闭的KCP连接实例
     */
    void handleClose(KcpConnection connection);

    /**
     * 空的监听器实现
     */
    KcpListener EMPTY = new KcpListener() {
        @Override
        public void onConnected(KcpConnection connection) {
            // 空实现
        }

        @Override
        public void handleReceive(ByteBuf data, KcpConnection connection) {
            // 空实现
        }

        @Override
        public void handleException(Throwable ex, KcpConnection connection) {
            // 空实现
        }

        @Override
        public void handleClose(KcpConnection connection) {
            // 空实现
        }
    };
}