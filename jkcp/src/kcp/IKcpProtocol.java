package kcp.kcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.List;

/**
 * KCP协议核心接口定义
 *
 * <p>定义了KCP协议的标准操作接口，包括数据收发、窗口管理、
 * 协议状态管理等核心功能。</p>
 *
 * @since 1.0
 */
public interface IKcpProtocol {

    /**
     * 释放KCP协议实例资源
     */
    void release();

    /**
     * 合并接收到的数据包
     * @return 合并后的ByteBuf
     */
    ByteBuf mergeRecv();

    /**
     * 接收数据
     *
     * <p>1. 判断是否有完整的包，如果有就抛给下一层</p>
     * <p>2. 整理消息接收队列，判断下一个包是否已经收到，收到放入rcvQueue</p>
     * <p>3. 判断接收窗口剩余是否改变，如果改变记录需要通知</p>
     *
     * @param bufList 接收缓冲区列表
     * @return 接收的数据大小
     */
    int recv(List<ByteBuf> bufList);

    /**
     * 检查接收队列中下一个完整消息的大小
     * @return -1 表示没有完整包，>0 表示一个完整包的字节长度
     */
    int peekSize();

    /**
     * 判断一条消息是否完整接收
     * @return true 如果消息完整，否则返回false
     */
    boolean canRecv();

    /**
     * 发送数据
     * @param buf 要发送的数据缓冲区
     * @return 发送的数据大小
     */
    int send(ByteBuf buf);

    /**
     * 输入数据到KCP协议栈
     *
     * @param data 接收到的数据包
     * @param regular 是否为常规数据包
     * @param current 当前时间戳（毫秒）
     * @return 处理结果
     */
    int input(ByteBuf data, boolean regular, long current);

    /**
     * 获取当前时间戳
     * @param now 当前时间戳
     * @return 处理后的当前时间戳
     */
    long currentMs(long now);

    /**
     * 刷新发送缓冲区
     *
     * <p>将待发送的数据包通过输出接口发送出去</p>
     *
     * @param ackOnly 是否只发送ACK包
     * @param current 当前时间戳（毫秒）
     * @return 下次更新时间
     */
    long flush(boolean ackOnly, long current);

    /**
     * 更新KCP协议状态
     *
     * <p>应该被定期调用（每10ms-100ms），或者通过ikcp_check判断下次调用时间</p>
     *
     * @param current 当前时间戳（毫秒）
     */
    void update(long current);

    /**
     * 检查下次更新时间
     *
     * <p>确定何时应该调用ikcp_update，返回距离下次更新的毫秒数。</p>
     * <p>用于减少不必要的ikcp_update调用，优化处理大量KCP连接的场景</p>
     *
     * @param current 当前时间戳（毫秒）
     * @return 下次更新需要等待的毫秒数
     */
    long check(long current);

    /**
     * 检查是否需要立即刷新
     * @return true 如果需要立即刷新
     */
    boolean checkFlush();

    /**
     * 设置最大传输单元
     * @param mtu MTU大小
     * @return 设置结果
     */
    int setMtu(int mtu);

    /**
     * 获取更新间隔
     * @return 更新间隔（毫秒）
     */
    int getInterval();

    /**
     * 设置传输模式
     *
     * @param nodelay 是否启用无延迟模式
     * @param interval 更新间隔
     * @param resend 快速重传次数
     * @param nc 是否关闭拥塞控制
     * @return 设置结果
     */
    int nodelay(boolean nodelay, int interval, int resend, boolean nc);

    /**
     * 获取待发送数据大小
     * @return 待发送数据大小
     */
    int waitSnd();

    /**
     * 获取连接ID
     * @return 连接ID
     */
    int getConv();

    /**
     * 设置连接ID
     * @param conv 连接ID
     */
    void setConv(int conv);

    /**
     * 获取用户数据
     * @return 用户数据对象
     */
    Object getUser();

    /**
     * 设置用户数据
     * @param user 用户数据对象
     */
    void setUser(Object user);

    /**
     * 获取协议状态
     * @return 协议状态
     */
    int getState();

    /**
     * 设置协议状态
     * @param state 协议状态
     */
    void setState(int state);

    /**
     * 是否启用无延迟模式
     * @return true 如果启用无延迟模式
     */
    boolean isNodelay();

    /**
     * 设置无延迟模式
     * @param nodelay 是否启用无延迟模式
     */
    void setNodelay(boolean nodelay);

    /**
     * 设置快速重传次数
     * @param fastresend 快速重传次数
     */
    void setFastresend(int fastresend);

    /**
     * 设置最小RTO
     * @param rxMinrto 最小RTO值
     */
    void setRxMinrto(int rxMinrto);

    /**
     * 设置接收窗口大小
     * @param rcvWnd 接收窗口大小
     */
    void setRcvWnd(int rcvWnd);

    /**
     * 设置ACK掩码大小
     * @param ackMaskSize ACK掩码大小
     */
    void setAckMaskSize(int ackMaskSize);

    /**
     * 设置保留字段
     * @param reserved 保留字段值
     */
    void setReserved(int reserved);

    /**
     * 获取发送窗口大小
     * @return 发送窗口大小
     */
    int getSndWnd();

    /**
     * 设置发送窗口大小
     * @param sndWnd 发送窗口大小
     */
    void setSndWnd(int sndWnd);

    /**
     * 是否启用流模式
     * @return true 如果启用流模式
     */
    boolean isStream();

    /**
     * 设置流模式
     * @param stream 是否启用流模式
     */
    void setStream(boolean stream);

    /**
     * 设置ByteBuf分配器
     * @param byteBufAllocator ByteBuf分配器
     */
    void setByteBufAllocator(ByteBufAllocator byteBufAllocator);

    /**
     * 获取输出处理器
     * @return KCP输出处理器
     */
    KcpOutput getOutput();

    /**
     * 设置输出处理器
     * @param output KCP输出处理器
     */
    void setOutput(KcpOutput output);

    /**
     * 设置ACK无延迟模式
     * @param ackNoDelay 是否启用ACK无延迟
     */
    void setAckNoDelay(boolean ackNoDelay);
}