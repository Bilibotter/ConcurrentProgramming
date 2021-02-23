package aqs;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: YHM
 * @Date: 2021/2/23 18:55
 */
public class TestDowngradeReadWriteLock {
    private final static DowngradeReadWriteLock lock = new DowngradeReadWriteLock();

    private final static DowngradeReadWriteLock.ReadLock read = lock.readerLock();

    private final static DowngradeReadWriteLock.WriteLock write = lock.writerLock();

    static class ReadWrite implements Runnable {
        @Override
        public void run() {
            write.lock();
            read.lock();
            System.out.println(Thread.currentThread().getName() + " get write lock and read lock!");
            read.unlock();
            write.unlock();
        }
    }

    // 更改释放锁的顺序
    static class ReadWrite2 implements Runnable {
        @Override
        public void run() {
            write.lock();
            read.lock();
            System.out.println(Thread.currentThread().getName() + " get write lock and read lock!");
            write.unlock();
            read.unlock();
        }
    }

    static class Write implements Runnable {
        @Override
        public void run() {
            write.lock();
            try {
                Thread.sleep(2000);
            }catch (InterruptedException ignore) {}
            write.unlock();
        }
    }

    static class RetryRead implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
            }catch (InterruptedException ignore) {}
            try {
                while (!read.tryLock(100, TimeUnit.MILLISECONDS)) {
                    System.out.println(Thread.currentThread().getName()+" acquire read lock fail!");
                };
            }
            catch (InterruptedException ignore) {}
            finally {
                read.unlock();
            }

        }
    }

    static class RetryWrite implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
            }catch (InterruptedException ignore) {}
            try {
                while (!write.tryLock(100, TimeUnit.MILLISECONDS)) {
                    System.out.println(Thread.currentThread().getName()+" acquire write lock fail!");
                };
            }
            catch (InterruptedException ignore) {}
            finally {
                write.unlock();
            }

        }
    }

    private static ThreadPoolExecutor getPool() {
        int processor = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(processor*2, processor*3, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(processor*10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        return exec;
    }

    private static void Test(Class runnable1, Class runnable2) throws Exception {
        int processor = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor exec = getPool();
        for (int i=0;i<processor*20;i++) {
            exec.execute((Runnable) runnable1.newInstance());
            exec.execute((Runnable) runnable2.newInstance());
        }
        exec.shutdown();
        while (!exec.isTerminated()) {}
        System.out.println("Finish!");
        Thread.sleep(2000);
    }

    private static void Test2(Class runnable1, Class runnable2) throws Exception {
        Thread t1 = new Thread((Runnable) runnable1.newInstance());
        Thread t2 = new Thread((Runnable) runnable2.newInstance());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Finish!");
        Thread.sleep(2000);
    }

    static class ExceedWriter implements Runnable {
        @Override
        public void run() {
            for (int i=0;;i++) {
                try {
                    write.lock();
                }catch (Error e) {
                    e.printStackTrace();
                    System.out.println("Success exceeded writer with lock time "+i);
                    break;
                }
            }
            for (;;) {
                try {
                    write.unlock();
                }catch (Error ignore) {
                    System.out.println("Release write lock.");
                    break;
                }
            }
            System.out.println("Finish!");
            try {
                Thread.sleep(1000);
            }catch (InterruptedException ignore) {}
        }
    }
    static class ExceedReader implements Runnable {
        @Override
        public void run() {
            for (int i=0;;i++) {
                try {
                    read.lock();
                }catch (Error e) {
                    e.printStackTrace();
                    System.out.println("Success exceeded reader with lock time "+i);
                    break;
                }
            }
            for (;;) {
                try {
                    read.unlock();
                }catch (Error ignore) {
                    System.out.println("Release read lock!");
                    break;
                }
            }
            // System.out.println(read.getCount());
            // System.out.println(read.getCount());
            // System.out.println(read.getTotal());
            // System.out.println(read.getTotal()>>16);
        }
    }

    public static void main(String[] args) throws Exception {
        // 多个线程不停获取读锁和写锁
        Test2(Write.class, RetryRead.class);
        Test2(Write.class, RetryWrite.class);
        // 先持有写锁时，读锁获取是否被阻塞
        Test(ReadWrite.class, ReadWrite.class);
        // 先持有写锁时，写锁获取是否被阻塞
        Test(ReadWrite.class, ReadWrite2.class);
        assert write.getHoldCount() == 0;
        Thread e1 = new Thread(new ExceedWriter());
        e1.start();
        e1.join();
        Thread e2 = new Thread(new ExceedReader());
        e2.start();
        e2.join();
    }
}
