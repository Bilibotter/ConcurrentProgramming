package aqs.test;

import java.util.concurrent.TimeUnit;

import static aqs.test.GlobalInstance.WRITE0;
import static aqs.test.GlobalInstance.READ0;

/**
 * @Author: YHM
 * @Date: 2021/2/25 12:11
 */
public class TestWriteExclusive0 {
    static class Write implements Runnable {
        @Override
        public void run() {
            WRITE0.lock();
            try {
                Thread.sleep(2000);
            }catch (InterruptedException ignore) {}
            System.out.println(Thread.currentThread().getName()+" release WRITE0 lock");
            WRITE0.unlock();
        }
    }

    static class RetryRead implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
            }catch (InterruptedException ignore) {}
            try {
                while (!READ0.tryLock(100, TimeUnit.MILLISECONDS)) {
                    System.out.println(Thread.currentThread().getName()+" acquire READ0 lock fail!");
                }
                System.out.println(Thread.currentThread().getName()+" acquire READ0 lock success!");
            }
            catch (InterruptedException ignore) {}
            finally {
                READ0.unlock();
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
                while (!WRITE0.tryLock(100, TimeUnit.MILLISECONDS)) {
                    System.out.println(Thread.currentThread().getName()+" acquire WRITE0 lock fail!");
                }
                System.out.println(Thread.currentThread().getName()+" acquire WRITE0 lock success!");
            }
            catch (InterruptedException ignore) {}
            finally {
                WRITE0.unlock();
            }

        }
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testOneTime("test write lock exclusive", Write.class, RetryWrite.class, RetryRead.class);
    }
}
