package kcp.core;

import kcp.core.FecStub.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * CRC32解码器
 *
 * <p>Netty管道的入站处理器，用于验证数据包的CRC32校验码。
 * 与Crc32Encoder配合使用，确保数据在网络传输过程中的完整性。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>校验码验证</strong>：验证数据包的CRC32校验码</li>
 *   <li><strong>数据完整性</strong>：确保数据在传输过程中未被篡改</li>
 *   <li><strong>错误检测</strong>：检测并拒绝损坏的数据包</li>
 *   <li><strong>性能优化</strong>：使用本地CRC32实例提高性能</li>
 * </ul>
 *
 * <p>处理流程：</p>
 * <pre>
 * 接收数据包 → 提取校验码 → 计算数据CRC32 → 比较校验码
 *     ↓
 * 校验成功 → 传递给下一个处理器
 * 校验失败 → 丢弃数据包并记录错误
 * </pre>
 *
 * <p>数据包格式：</p>
 * <pre>
 * +------------------+
 * |    数据载荷       |
 * +------------------+
 * |  CRC32校验码(4字节)|
 * +------------------+
 * </pre>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 在管道中添加解码器
 * ChannelPipeline pipeline = ch.pipeline();
 * pipeline.addLast("crc32Decoder", new Crc32Decode());
 * }</pre>
 *
 * <p>注意：</p>
 * <ul>
 *   <li>此处理器应与Crc32Encoder配对使用</li>
 *   <li>校验失败的数据包会被直接丢弃</li>
 *   <li>CRC32校验码应为小端序存储</li>
 *   <li>建议在协议处理器之前添加此解码器</li>
 * </ul>
 *
 * @since 1.0
 */
public class Crc32Decode extends ChannelInboundHandlerAdapter
{
    private CRC32 crc32 = new CRC32();
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DatagramPacket) {
            DatagramPacket datagramPacket = (DatagramPacket) msg;
            ByteBuf data = datagramPacket.content();
            long checksum =  data.readUnsignedIntLE();
            ByteBuffer byteBuffer = data.nioBuffer(data.readerIndex(),data.readableBytes());
            crc32.reset();
            crc32.update(byteBuffer);
            if(checksum!=crc32.getValue()){
                Snmp.snmp.getInCsumErrors().increment();
                return;
            }
        }
       ctx.fireChannelRead(msg);
    }
}
