package kcp.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * CRC32编码器
 *
 * <p>Netty管道的出站处理器，用于为数据包计算并添加CRC32校验码。
 * CRC32是一种高效的循环冗余校验算法，广泛用于数据完整性验证。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>校验码计算</strong>：为数据包计算CRC32校验码</li>
 *   <li><strong>数据追加</strong>：将校验码追加到数据包末尾</li>
 *   <li><strong>性能优化</strong>：使用本地CRC32实例提高性能</li>
 *   <li><strong>透明处理</strong>：对上层应用透明地处理校验逻辑</li>
 * </ul>
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
 * // 在管道中添加编码器
 * ChannelPipeline pipeline = ch.pipeline();
 * pipeline.addLast("crc32Encoder", new Crc32Encode());
 * }</pre>
 *
 * <p>注意：</p>
 * <ul>
 *   <li>此处理器应与其他处理器配合使用</li>
 *   <li>对应的使用Crc32Decode进行解码验证</li>
 *   <li>CRC32校验码为4字节，小端序存储</li>
 * </ul>
 *
 * @since 1.0
 */
public class Crc32Encode extends ChannelOutboundHandlerAdapter {

    private CRC32 crc32 = new CRC32();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        DatagramPacket datagramPacket = (DatagramPacket) msg;
        ByteBuf data = datagramPacket.content();
        ByteBuffer byteBuffer = data.nioBuffer(ChannelConfig.crc32Size,data.readableBytes()-ChannelConfig.crc32Size);
        crc32.reset();
        crc32.update(byteBuffer);
        long checksum = crc32.getValue();
        data.setIntLE(0, (int) checksum);
        //ByteBuf headByteBuf = ctx.alloc().ioBuffer(4);
        //headByteBuf.writeIntLE((int) checksum);
        //ByteBuf newByteBuf = Unpooled.wrappedBuffer(headByteBuf,data);
        //datagramPacket = datagramPacket.replace(newByteBuf);
        ctx.write(datagramPacket, promise);
    }
}
