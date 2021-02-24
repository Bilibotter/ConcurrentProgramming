package aqs;

import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @Author: YHM
 * @Date: 2021/2/22 10:45
 */
public class MyLatch {
    private final Sync sync;
    public MyLatch(int count) {
        sync = new Sync(count);
    }

    public int getCount() {
        return sync.getCount();
    }

    public void countDown() {
        sync.tryReleaseShared(1);
    }

    // 要设计成可中断
    public void await() throws InterruptedException {
        // sync.acquireShared(1);
        sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(time));
    }

    private static class Sync extends AbstractQueuedSynchronizer {
        public Sync(int count) {
            if (count < 0) {
                throw new IllegalMonitorStateException();
            }
            setState(count);
        }

        protected int getCount() {
            return getState();
        }

        @Override
        protected int tryAcquireShared(int arg) {
            return getState() == 0 ? 1:-1;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            for (;;) {
                int curr = getState();
                if (curr < arg) {
                    throw new IllegalMonitorStateException();
                }
                if (compareAndSetState(curr, curr-arg)) {
                    return true;
                }
            }
        }
    }
    private static final int COUNT = 100;
    private static final MyLatch latch = new MyLatch(COUNT);
    private static class Dec implements Runnable {
        @Override
        public void run() {
            try {
                latch.countDown();
                System.out.println(Thread.currentThread().getName()+" run count down.");
            }catch (IllegalMonitorStateException ignore) {
                System.out.println("Count has down to zero, do nothing.");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int nThreads = Runtime.getRuntime().availableProcessors()+1;
        ExecutorService exec =
                new ThreadPoolExecutor(nThreads, nThreads*2, 0L,
                        TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(nThreads * 10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        /*
        这种写法有点无聊
        for (int i=0;i<COUNT;i++) {
            exec.execute(new Dec());
        }
         */
        while (latch.getCount() > 0) {
            exec.execute(new Dec());
        }
        latch.await();
        System.out.println("Finish!");
        exec.shutdown();
    }
}
