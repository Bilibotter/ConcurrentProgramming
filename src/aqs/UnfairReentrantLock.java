package aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: YHM
 * @Date: 2021/2/21 15:46
 */
public class UnfairReentrantLock implements Lock {
    private final Sync sync = new Sync();

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean isHeldExclusively() {
            return getState() > 0;
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (getState() > 0) {
                if (Thread.currentThread() == getExclusiveOwnerThread()) {
                    return compareAndSetState(getState(), getState()+1);
                }
                return false;
            }
            if (getState() == 0) {
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }
            throw new IllegalMonitorStateException();
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() <= 0) {
                throw new IllegalMonitorStateException();
            } else if (Thread.currentThread() == getExclusiveOwnerThread()) {
                if (compareAndSetState(getState(), getState()-1)) {
                    if (getState() == 0) {
                        setExclusiveOwnerThread(null);
                    }
                    return true;
                }
            }
            return false;
        }

        Condition newCondition() {return new ConditionObject();}
    }

    private static class Test extends Thread {
        private final Lock lock;
        public Test(Lock lock) {
            super();
            this.lock = lock;
        }

        @Override
        public void run() {
            try {
                lock.lock();
                lock.lock();
                System.out.println(Thread.currentThread().getName()+" says:YHM hao nb!");
                Thread.sleep(500);
                lock.unlock();
                lock.unlock();
            }catch (InterruptedException ignore){}
        }
    }

    public static void main(String[] args) throws Exception{
        Thread[] threads = new Thread[13];
        Lock reentrantLock = new UnfairReentrantLock();
        for (int i=0;i<threads.length;i++) {
            threads[i] = new Test(reentrantLock);
            threads[i].start();
        }
        for (Thread thread:threads){
            thread.join();
        }
        System.out.println("Finish!");
    }
}
