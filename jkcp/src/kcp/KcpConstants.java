package kcp.kcp;

/**
 * KCP协议常量定义
 *
 * @since 1.0
 */
public final class KcpConstants {

    private KcpConstants() {
        // 工具类，禁止实例化
    }

    // ==================== RTO相关常量 ====================

    /**
     * No delay模式下最小RTO (毫秒)
     */
    public static final int IKCP_RTO_NDL = 30;

    /**
     * 正常模式下最小RTO (毫秒)
     */
    public static final int IKCP_RTO_MIN = 100;

    /**
     * 默认RTO (毫秒)
     */
    public static final int IKCP_RTO_DEF = 200;

    /**
     * 最大RTO (毫秒)
     */
    public static final int IKCP_RTO_MAX = 60000;

    // ==================== 发送窗口相关常量 ====================

    /**
     * 发送窗口大小
     */
    public static final int IKCP_WND_SND = 32;

    /**
     * 接收窗口大小
     */
    public static final int IKCP_WND_RCV = 128;

    // ==================== 协议头相关常量 ====================

    /**
     * 协议头大小 (字节)
     */
    public static final int IKCP_OVERHEAD = 24;

    /**
     * ACK推送数量
     */
    public static final int IKCP_ACK_PUSH = 64;

    // ==================== 传输模式常量 ====================

    /**
     * 普通模式 - 0
     */
    public static final int IKCP_MODE_NORMAL = 0;

    /**
     * 快速模式 - 1
     */
    public static final int IKCP_MODE_FAST = 1;

    /**
     * 最快模式 - 2
     */
    public static final int IKCP_MODE_FAST3 = 2;

    // ==================== 命令类型常量 ====================

    /**
     * 数据命令
     */
    public static final byte IKCP_CMD_PUSH = 81;

    /**
     * ACK命令
     */
    public static final byte IKCP_CMD_ACK = 82;

    /**
     * 窗口探测命令
     */
    public static final byte IKCP_CMD_WASK = 83;

    /**
     * 窗口通知命令
     */
    public static final byte IKCP_CMD_WINS = 84;

    // ==================== 状态标志常量 ====================

    /**
     * 询问远程窗口大小
     */
    public static final int IKCP_ASK_SEND = 1;

    /**
     * 通知远程窗口大小
     */
    public static final int IKCP_ASK_TELL = 2;

    // ==================== 时间相关常量 ====================

    /**
     * 默认时间间隔 (毫秒)
     */
    public static final int IKCP_INTERVAL = 100;

    /**
     * 快速重传触发次数
     */
    public static final int IKCP_FASTACK_LIMIT = 5;

    /**
     * 最小重传间隔 (毫秒)
     */
    public static final int IKCP_DEADLINK = 10;

    // ==================== 队列初始容量 ====================

    /**
     * ACK列表初始容量
     */
    public static final int INITIAL_ACK_LIST_CAPACITY = 128;

    /**
     * 缓冲区队列初始容量
     */
    public static final int INITIAL_BUFFER_CAPACITY = 32;
}