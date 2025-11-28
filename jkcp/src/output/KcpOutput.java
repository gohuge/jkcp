package kcp.output;

import kcp.kcp.IKcpProtocol;
import io.netty.buffer.ByteBuf;

/**
 * KCP协议数据输出接口
 *
 * <p>负责将KCP协议处理后的数据包发送到网络层。</p>
 *
 * @since 1.0
 */
public interface KcpOutput {

    /**
     * 输出数据包
     *
     * <p>将KCP协议处理后的数据包发送到网络层。</p>
     *
     * @param data 要发送的数据包
     * @param kcp KCP协议实例
     */
    void out(ByteBuf data, IKcpProtocol kcp);
}