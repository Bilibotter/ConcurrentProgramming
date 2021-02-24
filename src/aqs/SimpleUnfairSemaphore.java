package aqs;

import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @Author: YHM
 * @Date: 2021/2/22 11:55
 */
public class SimpleUnfairSemaphore {
    private final UnfairSync unfairSync;

    public SimpleUnfairSemaphore(int permit) {
        this.unfairSync = new UnfairSync(permit);
    }

    public boolean tryAcquire() {
        return unfairSync.tryAcquireShared(1) >= 0;
    }

    public void acquire() {
        unfairSync.acquireShared(1);
    }

    public void acquireInterruptibly() throws InterruptedException {
        unfairSync.acquireSharedInterruptibly(1);
    }

    public void acquireNanos(int time, TimeUnit unit) throws InterruptedException {
        unfairSync.tryAcquireSharedNanos(1, unit.toNanos(time));
    }

    public void release() {
        unfairSync.releaseShared(1);
    }

    private static class HoldCount extends ThreadLocal<Integer> {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    }

    private class UnfairSync extends AbstractQueuedSynchronizer {

        private HoldCount holdCount;

        public UnfairSync(int permit) {
            setState(permit);
            holdCount = new HoldCount();
        }

        // 还有许可就一直尝试, 直到没有许可
        @Override
        protected int tryAcquireShared(int arg) {
            for (;;) {
                int avail = getState();
                int remain = avail - arg;
                if (remain < 0) {
                    return remain;
                }
                if (compareAndSetState(avail, remain)) {
                    holdCount.set(holdCount.get() + 1);
                    return remain;
                }
            }
        }

        // 源码未修改同步状态的线程也能释放，感觉不安全
        @Override
        protected boolean tryReleaseShared(int arg) {
            int count = holdCount.get();
            if (count == 0) {
                throw new Error("Not locked by current thread");
            }
            for (;;) {
                int curr = getState();
                int next = curr + 1;
                // Java整数过大会溢出而变成负数
                if (next < 0) {
                    throw new Error("Maximum permit count exceeded");
                }
                if (compareAndSetState(curr, next)) {
                    holdCount.set(count - 1);
                    return true;
                }
            }
        }

        public int availablePermits() {
            return unfairSync.getState();
        }
    }

    private static class Acquire implements Runnable {
        @Override
        public void run() {
            SEMAPHORE.acquire();
            System.out.println(Thread.currentThread().getName()+" get permits!");
            SEMAPHORE.release();
        }
    }

    private static final int SEMAPHORE_NUM = Runtime.getRuntime().availableProcessors()+1;
    private static final SimpleUnfairSemaphore SEMAPHORE = new SimpleUnfairSemaphore(SEMAPHORE_NUM);

    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(10);
        System.out.println("Current available permits: "+semaphore.availablePermits());
        semaphore.release();
        System.out.println("After release without acquire before,permits is: "+semaphore.availablePermits());
        ThreadPoolExecutor exec =
                new ThreadPoolExecutor(SEMAPHORE_NUM*2, SEMAPHORE_NUM*4, 0L,
                        TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(SEMAPHORE_NUM*10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        for (int i=0; i<SEMAPHORE_NUM*20; i++) {
            exec.execute(new Acquire());
        }
        exec.shutdown();
        // 测试是否立刻关闭
        if (!exec.isTerminated()) {
            Thread.sleep(2000);
        }
        try {
            SEMAPHORE.release();
        } catch (Error error) {
            error.printStackTrace();
            Thread.sleep(10);
            System.out.println("Trig error correctly!");
        }
        System.out.println("Finish!");
    }
}
