package aqs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: YHM
 * @Date: 2021/2/24 20:45
 */
public class UpgradeReadWriteLock {
    final Sync sync;
    private final WriteLock writerLock;
    private final ReadLock readerLock;

    public UpgradeReadWriteLock() {
        this(false);
    }

    public UpgradeReadWriteLock(boolean fair) {
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
        private final Sync sync;

        public WriteLock(UpgradeReadWriteLock lock) {
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

        public int getStatuss() {
            return sync.writeState;
        }
    }

    public static class ReadLock implements Lock {
        private final Sync sync;

        public ReadLock(UpgradeReadWriteLock lock) {
            sync = lock.sync;
        }

        public int getStatusss() {
            return sync.getStatuss();
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
            } catch (IllegalAccessException| InvocationTargetException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        boolean writerShouldBlock() {
            return false;
        }
    }

    abstract static class Sync extends AbstractQueuedSynchronizer {
        // 写锁重入的数量
        private volatile int writeState = 0;
        // 二进制学的太渣了，不想了
        // 第19位记录写锁。后面和读锁有关
        private static final int SHIFT = 19;
        // 一个阈值类的，想好先
        private static final int SENTINEL = 1 << 19;
        // 最大容量还可以增加
        private static final int WRITE_MAX = 1 << 17;
        private static final int READ_MAX =  1 << 17;
        // 记录当前线程所持有的读锁数量
        private ThreadLocal<Integer> readCount;

        Sync() {
            readCount = new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };
            // 可能没有意义
            writeState = 0;
        }

        int getWriteState() {
            return writeState;
        }

        public int getStatuss() {
            return getState();
        }

        abstract boolean readerShouldBlock();

        abstract boolean writerShouldBlock();

        protected int getWriteHoldCount() {
            return isHeldExclusively()? 0: writeState;
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
            // 可以获取写锁的情况是当前为独占状态拥有者
            // 或state == 0
            if (isHeldExclusively()) {
                writeState += 1;
                // 溢出
                if (writeState >= WRITE_MAX) {
                    throw new Error("Maximum write lock count exceeded");
                }
                return true;
            }
            int c = getState();
            // sentinel机制并不好，但实现简单
            if (c == 0 && !writerShouldBlock() && compareAndSetState(c, c +SENTINEL)) {
                setExclusiveOwnerThread(Thread.currentThread());
                writeState += 1;
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!isHeldExclusively()) {
                throw new Error("Attempt to unlock write lock, not locked by current thread");
            }
            if (writeState != 1) {
                writeState--;
                return false;
            }
            // 写锁释放为串行
            // 写锁释放后由state而不是readCount计算读锁
            setState(getState()+readCount.get());
            setExclusiveOwnerThread(null);
            // happens-before
            writeState--;
            setState(getState()-SENTINEL);
            return true;
        }

        @Override
        protected int tryAcquireShared(int arg) {
            // 拥有写锁修改读锁状态不会有并发问题
            // 拥有写锁的状态下用ThreadLocal记录读锁数量
            if (isHeldExclusively()) {
                Integer lockNum = readCount.get();
                // 据说 == 比 >= 快
                if (lockNum == READ_MAX){
                    throw new Error("Maximum lock count exceeded");
                }
                readCount.set(lockNum + 1);
                return 1;
            }
            int c, nextc;
            for (;;) {
                c = getState();
                // 写锁已被持有
                if (c >= SENTINEL) {
                    return -1;
                }
                // 公平锁：前驱节点不是首节点
                // 非公平锁:独占节点的前驱节点为首节点
                if (readerShouldBlock()) {
                    return -1;
                }
                nextc = c + 1;
                if (nextc >= READ_MAX) {
                    throw new Error("Maximum read lock count exceeded");
                }
                if (compareAndSetState(c, nextc)) {
                    Integer lockNum = readCount.get();
                    readCount.set(lockNum + 1);
                    return 1;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            // 成功释放后该线程拥有的写锁数量
            Integer update = readCount.get() - 1;
            if (update <= -1) {
                throw new Error("Attempt to unlock read lock, not locked by current thread");
            }
            if (isHeldExclusively()) {
                readCount.set(update);
                if (update == 0) {
                    readCount.remove();
                }
                return update == 0;
            }
            int c, next;
            for (;;) {
                c = getState();
                next = c - 1;
                if (compareAndSetState(c, next)) {
                    readCount.set(update);
                    if (update == 0) {
                        readCount.remove();
                    }
                    // 写锁未被持有，则state==0意味着释放成功
                    return next == 0;
                }
            }
        }
    }
}
