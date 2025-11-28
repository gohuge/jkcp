package kcp.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端会话通道管理器
 *
 * <p>基于会话ID（conv）管理客户端KCP连接的实现类。
 * 适用于需要通过会话ID快速查找连接的场景。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>会话映射</strong>：基于conv会话ID管理连接</li>
 *   <li><strong>快速查找</strong>：O(1)时间复杂度的会话查找</li>
 *   <li><strong>线程安全</strong>：支持多线程并发访问</li>
 *   <li><strong>会话隔离</strong>：不同会话之间的连接完全隔离</li>
 * </ul>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li><strong>多会话应用</strong>：一个客户端同时维护多个会话</li>
 *   <li><strong>会话路由</strong>：根据会话ID路由到对应连接</li>
 *   <li><strong>负载均衡</strong>：多个连接分散在不同会话中</li>
 * </ul>
 *
 * @since 1.0
 */
public class ClientConvChannelManager implements IChannelManager {

    private int convIndex;

    public ClientConvChannelManager(int convIndex) {
        this.convIndex = convIndex;
    }

    private Map<Integer, Ukcp> ukcpMap = new ConcurrentHashMap<>();

    @Override
    public Ukcp get(DatagramPacket msg) {
        int conv = getConv(msg);
        return ukcpMap.get(conv);
    }


    private int getConv(DatagramPacket msg) {
        ByteBuf byteBuf = msg.content();
        return byteBuf.getIntLE(byteBuf.readerIndex() + convIndex);
    }

    @Override
    public void New(SocketAddress socketAddress, Ukcp ukcp, DatagramPacket msg) {
        int conv = ukcp.getConv();
        if (msg != null) {
            conv = getConv(msg);
            ukcp.setConv(conv);
        }

        ukcpMap.put(conv, ukcp);
    }

    @Override
    public void del(Ukcp ukcp) {
        ukcpMap.remove(ukcp.getConv());
        ukcp.user().getChannel().close();
    }

    @Override
    public Collection<Ukcp> getAll() {
        return this.ukcpMap.values();
    }
}
