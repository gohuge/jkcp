package kcp.fec;

import io.netty.buffer.ByteBuf;

/**
 * FEC（Forward Error Correction）前向纠错处理器
 *
 * <p>FEC是一种通过在发送端添加冗余数据，使接收端能够检测和纠正错误的
 * 数据传输技术，主要用于提高数据传输的可靠性。</p>
 *
 * @since 1.0
 */
public interface FecHandler {

    /**
     * FEC头部大小（+2）
     */
    int FEC_HEADER_SIZE_PLUS_2 = 0;

    /**
     * 编码数据
     * @param data 原始数据
     * @return 编码后的数据数组
     */
    ByteBuf[] encode(ByteBuf data);

    /**
     * 解码数据
     * @param data 接收到的数据
     * @return 解码后的数据
     */
    ByteBuf decode(ByteBuf data);

    /**
     * 释放资源
     */
    void release();

    /**
     * FEC适配器
     */
    interface FecAdapter {

        /**
         * 创建FEC编码器
         * @param headerSize 头部大小
         * @param mtu 最大传输单元
         * @return FEC编码器
         */
        FecHandler createEncoder(int headerSize, int mtu);

        /**
         * 创建FEC解码器
         * @param mtu 最大传输单元
         * @return FEC解码器
         */
        FecHandler createDecoder(int mtu);
    }

    /**
     * 空的FEC实现（用于禁用FEC功能）
     */
    FecHandler NO_OP_FEC = new FecHandler() {
        @Override
        public ByteBuf[] encode(ByteBuf data) {
            return new ByteBuf[]{data.retain()};
        }

        @Override
        public ByteBuf decode(ByteBuf data) {
            return data.retain();
        }

        @Override
        public void release() {
            // 空实现
        }
    };

    /**
     * FEC统计信息
     */
    class FecStatistics {
        private long totalEncodeCount;
        private long totalDecodeCount;
        private long errorCorrectionCount;

        public FecStatistics() {
        }

        public FecStatistics(long totalEncodeCount, long totalDecodeCount, long errorCorrectionCount) {
            this.totalEncodeCount = totalEncodeCount;
            this.totalDecodeCount = totalDecodeCount;
            this.errorCorrectionCount = errorCorrectionCount;
        }

        /**
         * 获取总编码次数
         */
        public long getTotalEncodeCount() {
            return totalEncodeCount;
        }

        /**
         * 获取总解码次数
         */
        public long getTotalDecodeCount() {
            return totalDecodeCount;
        }

        /**
         * 获取错误纠正次数
         */
        public long getErrorCorrectionCount() {
            return errorCorrectionCount;
        }

        /**
         * 增加编码计数
         */
        public void incrementEncodeCount() {
            totalEncodeCount++;
        }

        /**
         * 增加解码计数
         */
        public void incrementDecodeCount() {
            totalDecodeCount++;
        }

        /**
         * 增加错误纠正计数
         */
        public void incrementErrorCorrectionCount() {
            errorCorrectionCount++;
        }

        /**
         * 重置统计信息
         */
        public void reset() {
            totalEncodeCount = 0;
            totalDecodeCount = 0;
            errorCorrectionCount = 0;
        }

        @Override
        public String toString() {
            return String.format(
                "FEC Statistics - Encode: %d, Decode: %d, Error Corrections: %d",
                totalEncodeCount, totalDecodeCount, errorCorrectionCount
            );
        }
    }
}