package aqs.test;

import static aqs.test.GlobalInstance.*;

/**
 * @Author: YHM
 * @Date: 2021/2/25 11:18
 */
public class TestUnlockReadFirst0 implements Runnable {
    @Override
    public void run() {
        WRITE0.lock();
        READ0.lock();
        System.out.println(Thread.currentThread().getName() + " get write lock and read lock!");
        READ0.unlock();
        WRITE0.unlock();
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testWithThreadPool("compete read&write lock and release read first", TestUnlockReadFirst.class);
    }
}
