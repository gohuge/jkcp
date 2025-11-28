package kcp.core;

import com.backblaze.erasure.fec.Fec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import kcp.threading.IMessageExecutorPool;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KCP协议服务器实现
 *
 * <p>提供基于KCP协议的高性能UDP服务器功能，支持：
 * <ul>
 *   <li>多连接管理：同时处理大量客户端连接</li>
 *   <li>高性能I/O：支持Epoll、KQueue和NIO</li>
 *   <li>线程池管理：优化的线程池配置</li>
 *   <li>FEC支持：可选的前向纠错功能</li>
 *   <li>端口复用：支持同一端口多个连接</li>
 *   <li>自动调度：内置定时器和任务调度</li>
 * </ul>
 * </p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li><strong>高并发</strong>：支持数千个并发连接</li>
 *   <li><strong>低延迟</strong>：基于KCP协议的低延迟传输</li>
 *   <li><strong>高性能</strong>：使用Netty的高性能网络框架</li>
 *   <li><strong>跨平台</strong>：支持Linux、macOS、Windows</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 创建服务器
 * KcpServer server = new KcpServer();
 *
 * // 配置监听器
 * server.setKcpListener(new KcpListener() {
 *     // 实现回调方法
 * });
 *
 * // 启动服务器
 * server.start(8080);
 *
 * // 等待连接
 * server.join();
 * }</pre>
 *
 * @since 1.0
 */
public class KcpServer {
    private IMessageExecutorPool iMessageExecutorPool;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private List<Channel> localAddresss = new Vector<>();
    private IChannelManager channelManager;
    private HashedWheelTimer hashedWheelTimer;


    /**定时器线程工厂**/
    private static class TimerThreadFactory implements ThreadFactory
    {
        private AtomicInteger timeThreadName=new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r,"KcpServerTimerThread "+timeThreadName.addAndGet(1));
            return thread;
        }
    }

    //public void init(int workSize, KcpListener kcpListener, ChannelConfig channelConfig, int... ports) {
    //    DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool();
    //    for (int i = 0; i < workSize; i++) {
    //        disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool" + i);
    //    }
    //    init(disruptorExecutorPool, kcpListener, channelConfig, ports);
    //}


    public void init(KcpListener kcpListener, ChannelConfig channelConfig, int... ports) {
        if(channelConfig.isUseConvChannel()){
            int convIndex = 0;
            if(channelConfig.getFecAdapt()!=null){
                convIndex+= Fec.fecHeaderSizePlus2;
            }
            channelManager = new ServerConvChannelManager(convIndex);
        }else{
            channelManager = new ServerAddressChannelManager();
        }

        hashedWheelTimer = new HashedWheelTimer(new TimerThreadFactory(),1, TimeUnit.MILLISECONDS);


        boolean epoll = Epoll.isAvailable();
        boolean kqueue = KQueue.isAvailable();
        this.iMessageExecutorPool = channelConfig.getiMessageExecutorPool();
        bootstrap = new Bootstrap();
        int cpuNum = Runtime.getRuntime().availableProcessors();
        int bindTimes = 1;
        if (epoll||kqueue) {
            //ADD SO_REUSEPORT ？ https://www.jianshu.com/p/61df929aa98b
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bindTimes = cpuNum;
        }
        Class<? extends Channel> channelClass = null;
        if(epoll){
            group = new EpollEventLoopGroup(cpuNum);
            channelClass = EpollDatagramChannel.class;
        }else if(kqueue){
            group = new KQueueEventLoopGroup(cpuNum);
            channelClass = KQueueDatagramChannel.class;
        }else{
            group = new NioEventLoopGroup(ports.length);
            channelClass = NioDatagramChannel.class;
        }

        bootstrap.channel(channelClass);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ServerChannelHandler serverChannelHandler = new ServerChannelHandler(channelManager, channelConfig, iMessageExecutorPool, kcpListener,hashedWheelTimer);
                ChannelPipeline cp = ch.pipeline();
                if(channelConfig.isCrc32Check()){
                    Crc32Encode crc32Encode = new Crc32Encode();
                    Crc32Decode crc32Decode = new Crc32Decode();
                    //这里的crc32放在eventloop网络线程处理的，以后内核有丢包可以优化到单独的一个线程处理
                    cp.addLast(crc32Encode);
                    cp.addLast(crc32Decode);
                }
                cp.addLast(serverChannelHandler);
            }
        });
        //bootstrap.option(ChannelOption.SO_RCVBUF, 10*1024*1024);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);


        for (int port : ports) {
            for (int i = 0; i < bindTimes; i++) {
                ChannelFuture channelFuture = bootstrap.bind(port);
                Channel channel = channelFuture.channel();
                localAddresss.add(channel);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }

    public void stop() {
        localAddresss.forEach(
                channel -> channel.close()
        );
        channelManager.getAll().forEach(ukcp ->
                ukcp.close());
        if (iMessageExecutorPool != null) {
            iMessageExecutorPool.stop();
        }
        if(hashedWheelTimer!=null){
            hashedWheelTimer.stop();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public IChannelManager getChannelManager() {
        return channelManager;
    }
}
