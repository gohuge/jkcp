package kcp.core;

import com.backblaze.erasure.ReedSolomon;
import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fecNative.ReedSolomonNative;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import kcp.threading.IMessageExecutor;
import kcp.threading.IMessageExecutorPool;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KCP协议客户端实现
 *
 * <p>提供基于KCP协议的可靠UDP客户端功能，支持：
 * <ul>
 *   <li>自动连接管理：连接建立、维护和关闭</li>
 *   <li>消息处理：异步消息发送和接收</li>
 *   <li>线程管理：内置线程池和任务调度</li>
 *   <li>FEC支持：可选的前向纠错功能</li>
 *   <li>配置灵活：支持多种传输模式和参数配置</li>
 * </ul>
 * </p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 创建客户端
 * KcpClient client = new KcpClient();
 *
 * // 配置参数
 * client.setConv(1);
 * client.setNodelay(true, 10, 2, false);
 *
 * // 设置监听器
 * client.setKcpListener(new KcpListener() {
 *     // 实现回调方法
 * });
 *
 * // 启动客户端
 * client.start();
 *
 * // 连接服务器
 * KcpConnection connection = client.connect(new InetSocketAddress("localhost", 8080));
 * }</pre>
 *
 * @since 1.0
 */
public class KcpClient {


    private IMessageExecutorPool iMessageExecutorPool;
    private Bootstrap bootstrap;
    private EventLoopGroup nioEventLoopGroup;
    /**客户端的连接集合**/
    private IChannelManager channelManager;
    private HashedWheelTimer hashedWheelTimer;


    /**定时器线程工厂**/
    private static class TimerThreadFactory implements ThreadFactory
    {
        private AtomicInteger timeThreadName=new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r,"KcpClientTimerThread "+timeThreadName.addAndGet(1));
            return thread;
        }
    }

    public void init(ChannelConfig channelConfig) {
        if(channelConfig.isUseConvChannel()){
            int convIndex = 0;
            if(channelConfig.getFecAdapt()!=null){
                convIndex+= Fec.fecHeaderSizePlus2;
            }
            channelManager = new ClientConvChannelManager(convIndex);
        }else{
            channelManager = new ClientAddressChannelManager();
        }
        this.iMessageExecutorPool = channelConfig.getiMessageExecutorPool();
        nioEventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

        hashedWheelTimer = new HashedWheelTimer(new TimerThreadFactory(),1, TimeUnit.MILLISECONDS);

        bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.group(nioEventLoopGroup);
        bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline cp = ch.pipeline();
                if(channelConfig.isCrc32Check()){
                    Crc32Encode crc32Encode = new Crc32Encode();
                    Crc32Decode crc32Decode = new Crc32Decode();
                    cp.addLast(crc32Encode);
                    cp.addLast(crc32Decode);
                }
                cp.addLast(new ClientChannelHandler(channelManager));
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }



    /**
     * 重连接口
     * 使用旧的kcp对象，出口ip和端口替换为新的
     * 在4G切换为wifi等场景使用
     * @param ukcp
     */
    public void reconnect(Ukcp ukcp){
        if(!(channelManager instanceof ClientConvChannelManager)){
            throw new UnsupportedOperationException("reconnect can only be used in convChannel");
        }
        ukcp.getiMessageExecutor().execute(() -> {
            User user = ukcp.user();
            user.getChannel().close();
            InetSocketAddress  localAddress = new InetSocketAddress(0);
            ChannelFuture channelFuture = bootstrap.connect(user.getRemoteAddress(),localAddress);
            user.setChannel(channelFuture.channel());
        });
    }

    public Ukcp connect(InetSocketAddress localAddress,InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {
        if(localAddress==null){
            localAddress = new InetSocketAddress(0);
        }
        ChannelFuture channelFuture  = bootstrap.connect(remoteAddress,localAddress);

        //= bootstrap.bind(localAddress);
        ChannelFuture sync = channelFuture.syncUninterruptibly();
        NioDatagramChannel channel = (NioDatagramChannel) sync.channel();
        localAddress = channel.localAddress();

        User user = new User(channel, remoteAddress, localAddress);
        IMessageExecutor iMessageExecutor = iMessageExecutorPool.getIMessageExecutor();
        KcpOutput kcpOutput = new KcpOutputImpl();

        Ukcp ukcp = new Ukcp(kcpOutput, kcpListener, iMessageExecutor, channelConfig,channelManager);
        ukcp.user(user);

        channelManager.New(localAddress,ukcp,null);
        iMessageExecutor.execute(() -> {
            try {
                ukcp.getKcpListener().onConnected(ukcp);
            }catch (Throwable throwable){
                ukcp.getKcpListener().handleException(throwable,ukcp);
            }
        });

        ScheduleTask scheduleTask = new ScheduleTask(iMessageExecutor, ukcp,hashedWheelTimer);
        hashedWheelTimer.newTimeout(scheduleTask,ukcp.getInterval(),TimeUnit.MILLISECONDS);
        return ukcp;
    }

    public Ukcp connect(InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {
        return connect(null,remoteAddress,channelConfig,kcpListener);
    }


    public void stop() {
        //System.out.println("关闭连接");
        channelManager.getAll().forEach(ukcp -> {
            try {
                ukcp.close();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        //System.out.println("关闭连接1");
        if (iMessageExecutorPool != null) {
            iMessageExecutorPool.stop();
        }
        //System.out.println("关闭连接2");
        if (nioEventLoopGroup != null) {
            nioEventLoopGroup.shutdownGracefully();
        }
        if(hashedWheelTimer!=null){
            hashedWheelTimer.stop();
        }

        //System.out.println(Snmp.snmp);
        //System.out.println("关闭连接3");
    }

    public IChannelManager getChannelManager() {
        return channelManager;
    }
}
