package kcp.internal;

import java.util.Iterator;

/**
 * Reusable iterator
 *
 */
public interface ReusableIterator<E> extends Iterator<E> {

    /**
     * Reset the iterator to initial state.
     *
     * @return this object
     */
    ReusableIterator<E> rewind();

}
