package review;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: YHM
 * @Date: 2021/2/21 19:05
 */
public class AlterPrint3 {
    private final static Lock lock = new ReentrantLock();
    private final static Condition condition = lock.newCondition();
    static class PrintDigit extends Thread {
        @Override
        public void run() {
            for (int i=1;i<=26;i++) {
                lock.lock();
                System.out.println(i);
                condition.signal();
                lock.unlock();
                if (i==26) {
                    break;
                }
                try {
                    lock.lock();
                    condition.await();
                }
                catch (InterruptedException ignore) {}
                finally {
                    lock.unlock();
                }
            }
        }
    }
    static class PrintLetter extends Thread {
        @Override
        public void run() {
            for (int i = 0; i<26; i++) {
                lock.lock();
                System.out.println((char)(65+i));
                condition.signal();
                lock.unlock();
                if (i==25) {
                    break;
                }
                try {
                    lock.lock();
                    condition.await();
                }
                catch (InterruptedException ignore) {
                }
                finally {
                    lock.unlock();
                }
            }
        }
    }

    public static void main(String[] args) {
        new PrintDigit().start();
        new PrintLetter().start();
    }
}
