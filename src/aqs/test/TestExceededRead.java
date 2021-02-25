package aqs.test;

import static aqs.test.GlobalInstance.READ;

/**
 * @Author: YHM
 * @Date: 2021/2/25 10:46
 */
// 多线程重入写锁
public class TestExceededRead implements Runnable {
    @Override
    public void run() {
        for (int i=0;;i++) {
            try {
                READ.lock();
            } catch (Error error) {
                System.out.println(Thread.currentThread().getName()+" exceeded read in "+i+" lock time.");
                break;
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {}
        for (;;) {
            try {
                READ.unlock();
            } catch (Error error) {
                System.out.println(Thread.currentThread().getName()+" release read lock.");
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testOneTime("exceed read with single thread", TestExceededRead.class);
        Thread.sleep(1300);
        TestUtils.testWithThreadPool("exceed read with multi thread", TestExceededRead.class);
    }
}
