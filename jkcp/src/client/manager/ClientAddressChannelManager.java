package kcp.core;


import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端地址通道管理器
 *
 * <p>基于网络地址管理客户端KCP连接的实现类。
 * 使用ConcurrentHashMap提供线程安全的连接映射和查找功能。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>地址映射</strong>：基于服务器地址管理连接</li>
 *   <li><strong>线程安全</strong>：支持多线程并发访问</li>
 *   <li><strong>快速查找</strong>：O(1)时间复杂度的连接查找</li>
 *   <li><strong>内存效率</strong>：高效的内存使用和垃圾回收</li>
 * </ul>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li><strong>单客户端</strong>：一个客户端连接多个服务器</li>
 *   <li><strong>地址区分</strong>：基于服务器地址区分不同连接</li>
 *   <li><strong>并发访问</strong>：多线程环境下安全使用</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li><strong>无锁设计</strong>：使用ConcurrentHashMap避免锁竞争</li>
 *   <li><strong>高并发</strong>：支持大量并发连接管理</li>
 *   <li><strong>简单高效</strong>：最小化开销的实现方式</li>
 * </ul>
 *
 * @since 1.0
 */
public class ClientAddressChannelManager implements IChannelManager {
    private Map<SocketAddress, Ukcp> ukcpMap = new ConcurrentHashMap<>();

    @Override
    public Ukcp get(DatagramPacket msg) {
        return ukcpMap.get(msg.recipient());
    }

    @Override
    public void New(SocketAddress socketAddress, Ukcp ukcp, DatagramPacket msg) {
        ukcpMap.put(socketAddress, ukcp);
    }

    @Override
    public void del(Ukcp ukcp) {
        ukcpMap.remove(ukcp.user().getLocalAddress());
        ukcp.user().getChannel().close();
    }

    @Override
    public Collection<Ukcp> getAll() {
        return this.ukcpMap.values();
    }
}
