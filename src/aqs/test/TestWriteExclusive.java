package aqs.test;

import java.util.concurrent.TimeUnit;

import static aqs.test.GlobalInstance.WRITE;
import static aqs.test.GlobalInstance.READ;

/**
 * @Author: YHM
 * @Date: 2021/2/25 12:11
 */
public class TestWriteExclusive {
    static class Write implements Runnable {
        @Override
        public void run() {
            WRITE.lock();
            try {
                Thread.sleep(2000);
            }catch (InterruptedException ignore) {}
            System.out.println(Thread.currentThread().getName()+" release WRITE lock");
            WRITE.unlock();
        }
    }

    static class RetryRead implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
            }catch (InterruptedException ignore) {}
            try {
                while (!READ.tryLock(100, TimeUnit.MILLISECONDS)) {
                    System.out.println(Thread.currentThread().getName()+" acquire READ lock fail!");
                }
                System.out.println(Thread.currentThread().getName()+" acquire READ lock success!");
            }
            catch (InterruptedException ignore) {}
            finally {
                READ.unlock();
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
                while (!WRITE.tryLock(100, TimeUnit.MILLISECONDS)) {
                    System.out.println(Thread.currentThread().getName()+" acquire WRITE lock fail!");
                }
                System.out.println(Thread.currentThread().getName()+" acquire WRITE lock success!");
            }
            catch (InterruptedException ignore) {}
            finally {
                WRITE.unlock();
            }

        }
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testOneTime("test write lock exclusive", Write.class, RetryWrite.class, RetryRead.class);
    }
}
