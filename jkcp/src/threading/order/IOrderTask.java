package kcp.threading.order;

/**
 * 2020/6/19.
 */
public interface IOrderTask extends Runnable{

    OrderedThreadSession getSession();
}
