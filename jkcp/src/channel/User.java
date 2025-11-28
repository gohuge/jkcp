package kcp.core;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * KCP用户连接信息封装类
 *
 * <p>封装KCP连接的网络层信息，包括Netty通道、网络地址等。
 * 作为KCP协议实例与网络传输层之间的桥梁。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>通道管理</strong>：管理Netty网络通道</li>
 *   <li><strong>地址信息</strong>：存储本地和远程网络地址</li>
 *   <li><strong>缓存支持</strong>：提供连接级别的数据缓存</li>
 *   <li><strong>状态管理</strong>：维护连接的基本状态信息</li>
 * </ul>
 *
 * <p>数据结构：</p>
 * <pre>
 * User {
 *     Channel channel;          // Netty网络通道
 *     InetSocketAddress remote;  // 远程网络地址
 *     InetSocketAddress local;   // 本地网络地址
 *     Object cache;             // 用户缓存数据
 * }
 * </pre>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li><strong>连接建立</strong>：创建新连接时初始化网络信息</li>
 *   <li><strong>数据发送</strong>：发送数据时获取网络地址</li>
 *   <li><strong>状态查询</strong>：查询连接的网络状态</li>
 *   <li><strong>资源管理</strong>：连接关闭时清理网络资源</li>
 * </ul>
 *
 * <p>设计特点：</p>
 * <ul>
 *   <li><strong>轻量级</strong>：最小化内存占用和对象创建</li>
 *   <li><strong>高效</strong>：直接存储网络地址，避免重复查找</li>
 *   <li><strong>灵活</strong>：支持用户自定义缓存数据</li>
 *   <li><strong>安全</strong>：提供基本的数据保护机制</li>
 * </ul>
 *
 * @since 1.0
 */
public class User {

    private Channel channel;
    private InetSocketAddress remoteAddress;
    private InetSocketAddress localAddress;

    private Object cache;

    public void setCache(Object cache) {
        this.cache = cache;
    }

    public <T>  T getCache() {
        return (T) cache;
    }

    public User(Channel channel, InetSocketAddress remoteAddress, InetSocketAddress localAddress) {
        this.channel = channel;
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
    }

    protected Channel getChannel() {
        return channel;
    }

    protected void setChannel(Channel channel) {
        this.channel = channel;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    protected void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    protected void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }


    @Override
    public String toString() {
        return "User{" +
                "remoteAddress=" + remoteAddress +
                ", localAddress=" + localAddress +
                '}';
    }
}
