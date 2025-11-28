package kcp.core;

import kcp.core.FecStub.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;


/**
 * KCP网络输出处理器实现
 *
 * <p>实现KcpOutput接口，负责将KCP协议处理后的数据包通过网络发送出去。
 * 这是KCP协议与底层网络传输层之间的桥梁。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>数据包发送</strong>：将KCP数据包封装为UDP数据报发送</li>
 *   <li><strong>统计信息</strong>：记录发送的数据包数量和字节数</li>
 *   <li><strong>地址处理</strong>：处理源地址和目标地址</li>
 *   <li><strong>异步发送</strong>：使用Netty的异步I/O机制发送数据</li>
 * </ul>
 *
 * <p>处理流程：</p>
 * <pre>
 * KCP数据包 → 更新统计信息 → 封装为UDP包 → 异步发送
 * </pre>
 *
 * <p>性能特点：</p>
 * <ul>
 *   <li><strong>零拷贝</strong>：直接使用ByteBuf，避免数据拷贝</li>
 *   <li><strong>异步处理</strong>：不阻塞KCP协议处理线程</li>
 *   <li><strong>统计监控</strong>：提供发送性能的统计数据</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 创建KCP连接时设置输出处理器
 * KcpOutput output = new KcpOutputHandler();
 * kcp.setOutput(output);
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>此类通常由框架自动创建和使用</li>
 *   <li>发送操作是异步的，可能不会立即完成</li>
 *   <li>统计信息用于性能监控和调试</li>
 *   <li>确保ByteBuf在使用完毕前不被释放</li>
 * </ul>
 *
 * @since 1.0
 */
public class KcpOutputImpl implements KcpOutput {

    @Override
    public void out(ByteBuf data, IKcp kcp) {
        Snmp.snmp.OutPkts.increment();
        Snmp.snmp.OutBytes.add(data.writerIndex());
        User user = (User) kcp.getUser();
        DatagramPacket temp = new DatagramPacket(data,user.getRemoteAddress(), user.getLocalAddress());
        user.getChannel().writeAndFlush(temp);
    }
}
