package aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @Author: YHM
 * @Date: 2021/2/22 20:59
 */
public class UnfairReadWriteLock {
    // private final UnfairSync sync = new UnfairSync();

    private abstract static class UnfairSync extends AbstractQueuedSynchronizer {
        // 低16位为写状态，高16位为读状态
        private static final int SHIFT = 16;
        // 与0xffff做并运算即为写状态
        private static final int WRITE_MASK = (1 << 16)-1;
        // 读锁的增量单位
        private static final int READ_UNIT = 1 << 16;

        int getExclusive(int state) {
            return state & WRITE_MASK;
        }

        int getShared(int state) {
            return state >> SHIFT;
        }

        @Override
        protected boolean isHeldExclusively() {
            return Thread.currentThread() == getExclusiveOwnerThread();
        }

        abstract boolean readerShouldBlock();

        abstract boolean writerShouldBlock();

        @Override
        protected boolean tryAcquire(int arg) {
            // 这里非常精妙,可能优于官方
            int state = getState();
            // 可以获取写锁的情况是state == 0
            // 或为独占状态拥有者
            if (state == 0 && !writerShouldBlock()) {
                return compareAndSetState(state, state+arg);
            }
            if (isHeldExclusively()) {
                int update = getExclusive(state) + arg;
                if (update > WRITE_MASK) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(update);
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            // 自己的二进制学的太菜了
            int update = getState() - arg;
            setState(update);
            if (getExclusive(update) == 0) {
                setExclusiveOwnerThread(null);
                return true;
            }
            return false;
        }

        /*
        @Override
        protected boolean tryReleaseShared(int arg) {
            int state = getState();
            if (isHeldExclusively()) {
                setState(state+(arg<<SHIFT));
                return true;
            }
            int exclusiveNum = getExclusive(getState());
            if (exclusiveNum > 0) {
                return false;
            }
        }

         */
    }
}
