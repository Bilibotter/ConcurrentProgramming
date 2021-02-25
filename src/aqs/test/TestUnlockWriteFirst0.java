package aqs.test;

import static aqs.test.GlobalInstance.READ0;
import static aqs.test.GlobalInstance.WRITE0;

/**
 * @Author: YHM
 * @Date: 2021/2/25 11:55
 */
public class TestUnlockWriteFirst0 implements Runnable {
    @Override
    public void run() {
        WRITE0.lock();
        READ0.lock();
        System.out.println(Thread.currentThread().getName() + " get write lock and read lock!");
        WRITE0.unlock();
        READ0.unlock();
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testWithThreadPool("compete read&write lock and release write first", TestUnlockWriteFirst0.class);
    }
}
