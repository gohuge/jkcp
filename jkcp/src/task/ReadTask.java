package kcp.core;

import kcp.core.FecStub.Snmp;
import kcp.internal.CodecOutputList;
import io.netty.buffer.ByteBuf;
import kcp.threading.ITask;

import java.util.Queue;

/**
 * KCP读取任务
 *
 * <p>实现ITask接口，负责处理KCP连接的数据读取操作。
 * 作为KCP异步任务调度系统的一部分，在专门的线程中执行。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li><strong>数据读取</strong>：从KCP接收缓冲区读取完整的数据包</li>
 *   <li><strong>数据传递</strong>：将读取的数据传递给应用层处理</li>
 *   <li><strong>状态管理</strong>：管理读取任务的执行状态</li>
 *   <li><strong>性能监控</strong>：记录读取操作的统计数据</li>
 * </ul>
 *
 * <p>执行流程：</p>
 * <pre>
 * 获取读锁 → 检查缓冲区 → 读取数据包 → 调用接收回调 → 更新状态
 * </pre>
 *
 * <p>任务特点：</p>
 * <ul>
 *   <li><strong>异步执行</strong>：在专门的读取线程中执行，不阻塞网络I/O</li>
 *   <li><strong>批量处理</strong>：支持一次性读取多个数据包</li>
 *   <li><strong>错误处理</strong>：具备完善的异常处理机制</li>
 *   <li><strong>资源管理</strong>：确保ByteBuf的正确释放</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 创建读取任务
 * ReadTask readTask = new ReadTask(kcpConnection);
 *
 * // 提交给任务执行器
 * messageExecutor.execute(readTask);
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>此类由框架内部管理，通常不需要直接创建</li>
 *   <li>确保在任务执行期间连接保持活跃状态</li>
 *   <li>妥善处理异常情况，避免影响其他任务</li>
 *   <li>合理控制读取频率，避免过度消耗CPU资源</li>
 * </ul>
 *
 * @since 1.0
 */
public class ReadTask implements ITask {


    private final Ukcp ukcp;

    public ReadTask(Ukcp ukcp) {
        this.ukcp = ukcp;
    }


    @Override
    public void execute() {
        CodecOutputList<ByteBuf> bufList = null;
        Ukcp ukcp = this.ukcp;
        try {
            //查看连接状态
            if (!ukcp.isActive()) {
                return;
            }
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = ukcp.getReadBuffer();
            int readCount =0;
            for (; ; ) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                readCount++;
                ukcp.input(byteBuf, current);
                byteBuf.release();
            }
            if (readCount==0) {
                return;
            }
            if(ukcp.isControlReadBufferSize()){
                ukcp.getReadBufferIncr().addAndGet(readCount);
            }
            long readBytes = 0;
            if (ukcp.isStream()) {
                int size =0;
                while (ukcp.canRecv()) {
                    if (bufList == null) {
                        bufList = CodecOutputList.newInstance();
                    }
                    ukcp.receive(bufList);
                    size= bufList.size();
                }
                for (int i = 0; i < size; i++) {
                    ByteBuf byteBuf = bufList.getUnsafe(i);
                    readBytes += byteBuf.readableBytes();
                    readBytebuf(byteBuf,current,ukcp);
                }
            } else {
                while (ukcp.canRecv()) {
                    ByteBuf recvBuf = ukcp.mergeReceive();
                    readBytes += recvBuf.readableBytes();
                    readBytebuf(recvBuf,current,ukcp);
                }
            }
            Snmp.snmp.BytesReceived.add(readBytes);
            //判断写事件
            if (!ukcp.getWriteBuffer().isEmpty()&& ukcp.canSend(false)) {
                ukcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            ukcp.internalClose();
            e.printStackTrace();
        } finally {
            release();
            if (bufList != null) {
                bufList.recycle();
            }
        }
    }


    private void readBytebuf(ByteBuf buf,long current,Ukcp ukcp) {
        ukcp.setLastRecieveTime(current);
        try {
            ukcp.getKcpListener().handleReceive(buf, ukcp);
        } catch (Throwable throwable) {
            ukcp.getKcpListener().handleException(throwable, ukcp);
        }finally {
            buf.release();
        }
    }

    public void release() {
        ukcp.getReadProcessing().set(false);
    }

}
