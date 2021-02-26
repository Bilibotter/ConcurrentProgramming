package aqs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: YHM
 * @Date: 2021/2/23 21:26
 */
public class DowngradeReadWriteLock {
    final Sync sync;
    private final WriteLock writerLock;
    private final ReadLock readerLock;

    public DowngradeReadWriteLock() {
        this(false);
    }

    public DowngradeReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new UnfairSync();
        writerLock = new WriteLock(this);
        readerLock = new ReadLock(this);
    }

    public WriteLock writerLock() {
        return writerLock;
    }

    public ReadLock readerLock() {
        return readerLock;
    }

    public static class WriteLock implements Lock {
        private final DowngradeReadWriteLock.Sync sync;

        public WriteLock(DowngradeReadWriteLock lock) {
            sync = lock.sync;
        }

        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }

        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }
    }

    public static class ReadLock implements Lock {
        private final Sync sync;

        public ReadLock(DowngradeReadWriteLock lock) {
            sync = lock.sync;
        }

        @Override
        public void lock() {
            sync.acquireShared(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquireShared(1) == 1;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(time));
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        @Override
        public void unlock() {
            sync.releaseShared(1);
        }

        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }
    }

    static final class FairSync extends Sync {
        @Override
        boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }

        @Override
        boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    static final class UnfairSync extends Sync {
        protected Method apparentlyFirstQueuedIsExclusive;
        UnfairSync() {
            super();
            // apparentlyFirstQueuedIsExclusive不可访问，通过反射调用
            try {
                Class<?> aqs = Class.forName(AbstractQueuedSynchronizer.class.getName());
                apparentlyFirstQueuedIsExclusive = aqs.getDeclaredMethod("apparentlyFirstQueuedIsExclusive");
                apparentlyFirstQueuedIsExclusive.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchMethodException ignore){}
        }
        @Override
        boolean readerShouldBlock() {
            try {
                return (boolean) apparentlyFirstQueuedIsExclusive.invoke(this);
            } catch (IllegalAccessException| InvocationTargetException ignore) {}
            return false;
        }

        @Override
        boolean writerShouldBlock() {
            return false;
        }
    }

    private static class ReadCount extends ThreadLocal<Integer> {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    }

    abstract static class Sync extends AbstractQueuedSynchronizer {
        // 对写锁容量降级以获取更好的性能
        // 同时写锁降级后会程序出错的地方更快触发
        // 低15位为写状态，高16位为读状态
        private static final int SHIFT = 15;
        // 与0x7fff做并运算即为写状态
        private static final int WRITE_MASK = (1 << SHIFT)-1;
        // 读锁的增量单位
        private static final int READ_UNIT = (1 << SHIFT);
        // Java整数过大会出现负数
        // 因此降级写锁简化逻辑与提高性能
        private static final int WRITE_MAX = WRITE_MASK;
        private static final int READ_MAX = (1<<16)-1;
        // 记录当前线程所持有的读锁数量
        private ThreadLocal<Integer> readCount;

        Sync() {
            readCount = new ReadCount();
        }

        int getExclusive(int c) {
            return c & WRITE_MASK;
        }

        int getShared(int c) {
            return c >> SHIFT;
        }

        abstract boolean readerShouldBlock();

        abstract boolean writerShouldBlock();

        protected int getWriteHoldCount() {
            return isHeldExclusively()? 0: getExclusive(getState());
        }

        protected Condition newCondition() {
            return new ConditionObject();
        }

        @Override
        protected boolean isHeldExclusively() {
            return Thread.currentThread() == getExclusiveOwnerThread();
        }

        @Override
        protected boolean tryAcquire(int arg) {
            // 可能优于官方
            int c = getState();
            // 可以获取写锁的情况是当前为独占状态拥有者
            // 或state == 0
            if (isHeldExclusively()) {
                int update = getExclusive(c) + 1;
                if (update > WRITE_MAX) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(update);
                return true;
            }
            if (c == 0 && !writerShouldBlock() && compareAndSetState(c, c +1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!isHeldExclusively()) {
                throw new Error("Unmatched unlock!");
            }
            // 自己的二进制学的太菜了
            int update = getState() - 1;
            setState(update);
            if (getExclusive(update) == 0) {
                setExclusiveOwnerThread(null);
                return true;
            }
            return false;
        }

        @Override
        protected int tryAcquireShared(int arg) {
            int update = readCount.get() + 1;
            // 拥有写锁必成功
            if (isHeldExclusively()) {
                // 拥有写锁，则读锁总数 == 当前线程读锁数
                // 不用根据State正负转化后计算
                if (update >= READ_MAX){
                    throw new Error("Maximum lock count exceeded");
                }
                readCount.set(update);
                setState(getState() + READ_UNIT);
                return 1;
            }
            // 公平锁：前驱节点不是首节点
            // 非公平锁:独占节点的前驱节点为首节点
            int c, nextc;
            boolean noLock = update == 1;
            for (;;) {
                c = getState();
                // 已持有读锁不该被阻塞
                if (noLock && readerShouldBlock()) {
                    return -1;
                }
                if (getExclusive(c) > 0) {
                    return -1;
                }
                nextc = c + READ_UNIT;
                if (nextc >= READ_MAX) {
                    throw new Error("Maximum lock count exceeded");
                }
                if (compareAndSetState(c, nextc)) {
                    readCount.set(update);
                    return 1;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            int lockNum = readCount.get();
            if (lockNum == 0) {
                throw new Error("Unmatched unlock!");
            }
            if (isHeldExclusively()) {
                if (lockNum == 1) {
                    readCount.remove();
                }
                readCount.set(lockNum-1);
                setState(getState() - READ_UNIT);
                return lockNum == 0;
            }
            int c, next;
            for (;;) {
                c = getState();
                next = c - READ_UNIT;
                // 锁降级前面已经处理过了，因此写锁必为0
                if (compareAndSetState(c, next)) {
                    if (lockNum == 1) {
                        readCount.remove();
                    }
                    readCount.set(lockNum-1);
                    return next == 0;
                }
            }
        }
    }
}
