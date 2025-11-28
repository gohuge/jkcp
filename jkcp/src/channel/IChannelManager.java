package kcp.core;

import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;


/**
 * KCP通道管理器接口
 *
 * <p>定义了KCP通道管理的标准接口，负责管理网络连接与KCP协议实例之间的映射关系。
 * 支持基于地址和会话ID的连接查找和管理。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>连接映射</strong>：建立网络地址与KCP连接的映射关系</li>
 *   <li><strong>连接查找</strong>：根据地址或会话ID快速查找KCP连接</li>
 *   <li><strong>生命周期</strong>：管理连接的创建、维护和销毁</li>
 *   <li><strong>集合操作</strong>：提供连接集合的查询和遍历功能</li>
 * </ul>
 *
 * <p>管理策略：</p>
 * <ul>
 *   <li><strong>地址映射</strong>：基于网络地址的连接管理</li>
 *   <li><strong>会话管理</strong>：基于会话ID的连接管理</li>
 *   <li><strong>混合模式</strong>：同时支持地址和会话两种管理方式</li>
 * </ul>
 *
 * <p>接口设计特点：</p>
 * <ul>
 *   <li><strong>线程安全</strong>：所有操作必须是线程安全的</li>
 *   <li><strong>高性能</strong>：支持快速的查找和更新操作</li>
 *   <li><strong>可扩展</strong>：支持不同的管理策略实现</li>
 *   <li><strong>容错性</strong>：具备完善的错误处理机制</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 创建通道管理器
 * IChannelManager channelManager = new AddressChannelManager();
 *
 * // 接收到新数据包时查找连接
 * Ukcp connection = channelManager.get(datagramPacket);
 * if (connection == null) {
 *     // 创建新连接
 *     connection = createNewConnection(datagramPacket);
 *     channelManager.New(address, connection, datagramPacket);
 * }
 *
 * // 连接关闭时移除
 * channelManager.del(connection);
 * }</pre>
 *
 * <p>实现注意事项：</p>
 * <ul>
 *   <li>确保并发访问时的线程安全性</li>
 *   <li>合理设计数据结构，优化查找性能</li>
 *   <li>处理网络地址变化的情况</li>
 *   <li>定期清理无效或超时的连接</li>
 * </ul>
 *
 * @since 1.0
 */
public interface IChannelManager {

    Ukcp get(DatagramPacket msg);

    void New(SocketAddress socketAddress,Ukcp ukcp,DatagramPacket msg);

    void del(Ukcp ukcp);

    Collection<Ukcp> getAll();
}
