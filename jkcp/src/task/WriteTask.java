package kcp.core;

import kcp.core.FecStub.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.threading.ITask;

import java.io.IOException;
import java.util.Queue;

/**
 * KCP写入任务
 *
 * <p>实现ITask接口，负责处理KCP连接的数据写入操作。
 * 作为KCP异步任务调度系统的一部分，在专门的线程中执行数据发送。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>数据发送</strong>：从写入缓冲区获取数据并发送到网络</li>
 *   <li><strong>批量处理</strong>：支持一次性发送多个数据包</li>
 *   <li><strong>流控制</strong>：根据网络状况调整发送速率</li>
 *   <li><strong>错误处理</strong>：处理网络发送异常情况</li>
 * </ul>
 *
 * <p>执行流程：</p>
 * <pre>
 * 获取写锁 → 检查缓冲区 → 准备数据包 → 调用KCP发送 → 释放资源
 * </pre>
 *
 * <p>任务特点：</p>
 * <ul>
 *   <li><strong>异步执行</strong>：在专门的写入线程中执行，不阻塞网络I/O</li>
 *   <li><strong>高性能</strong>：批量发送减少系统调用开销</li>
 *   <li><strong>可靠性</strong>：确保数据正确发送到网络层</li>
 *   <li><strong>可扩展</strong>：支持不同的发送策略和优化</li>
 * </ul>
 *
 * @since 1.0
 */
public class WriteTask implements ITask {


    private final Ukcp ukcp;

    public WriteTask(Ukcp ukcp) {
        this.ukcp = ukcp;
    }

    @Override
    public void execute() {
        Ukcp ukcp = this.ukcp;
        try {
            //查看连接状态
            if(!ukcp.isActive()){
                return;
            }
            //从发送缓冲区到kcp缓冲区
            Queue<ByteBuf> queue = ukcp.getWriteBuffer();
            int writeCount =0;
            long writeBytes = 0;
            while(ukcp.canSend(false)){
                ByteBuf byteBuf = queue.poll();
                if(byteBuf==null){
                    break;
                }
                writeCount++;
                try {
                    writeBytes +=byteBuf.readableBytes();
                    ukcp.send(byteBuf);
                    byteBuf.release();
                } catch (IOException e) {
                    ukcp.getKcpListener().handleException(e, ukcp);
                    return;
                }
            }
            Snmp.snmp.BytesSent.add(writeBytes);
            if(ukcp.isControlWriteBufferSize()){
                ukcp.getWriteBufferIncr().addAndGet(writeCount);
            }
            //如果有发送 则检测时间
            if(!ukcp.canSend(false)||(ukcp.checkFlush()&& ukcp.isFastFlush())){
                long now =System.currentTimeMillis();
                long next = ukcp.flush(now);
                ukcp.setTsUpdate(now+next);
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            release();
        }
    }


    public void release(){
        ukcp.getWriteProcessing().set(false);
    }
}
