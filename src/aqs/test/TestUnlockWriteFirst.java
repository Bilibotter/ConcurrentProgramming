package aqs.test;

import static aqs.test.GlobalInstance.READ;
import static aqs.test.GlobalInstance.WRITE;

/**
 * @Author: YHM
 * @Date: 2021/2/25 11:55
 */
public class TestUnlockWriteFirst implements Runnable {
    @Override
    public void run() {
        WRITE.lock();
        READ.lock();
        System.out.println(Thread.currentThread().getName() + " get write lock and read lock!");
        WRITE.unlock();
        READ.unlock();
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testWithThreadPool("compete read&write lock and release write first", TestUnlockWriteFirst.class);
    }
}
