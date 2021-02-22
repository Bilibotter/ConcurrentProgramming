package aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: YHM
 * @Date: 2021/2/21 20:10
 */
public class FairReentrantLock implements Lock {
    private final Sync sync = new Sync();
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
    public void unlock() {
        sync.release(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryRelease(int arg) {
            int newState = getState() - arg;
            if (Thread.currentThread() == getExclusiveOwnerThread()) {
                if (newState == 0) {
                    setExclusiveOwnerThread(null);
                }
                setState(newState);
                return newState == 0;
            }
            throw new IllegalMonitorStateException();
        }

        @Override
        protected boolean tryAcquire(int arg) {
            int state = getState();
            if (state == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            }
            else if (state > 0) {
                if (Thread.currentThread() != getExclusiveOwnerThread()) {
                    return false;
                }
                setState(state+arg);
                return true;
            }
            throw new IllegalMonitorStateException();
        }

        Condition newCondition() {return new ConditionObject();}

        public static void main(String[] args) throws Exception{
            Thread[] threads = new Thread[13];
            Lock reentrantLock = new FairReentrantLock();
            for (int i=0;i<threads.length;i++) {
                threads[i] = new UnfairReentrantLock.Test(reentrantLock);
                threads[i].start();
            }
            for (Thread thread:threads){
                thread.join();
            }
            System.out.println("Finish!");
        }
    }
}
