package aqs.test;

import static aqs.test.GlobalInstance.READ0;

/**
 * @Author: YHM
 * @Date: 2021/2/25 11:07
 */
public class TestExceededRead0 implements Runnable{
    @Override
    public void run() {
        for (int i=0;;i++) {
            try {
                READ0.lock();
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
                READ0.unlock();
            } catch (Error error) {
                System.out.println(Thread.currentThread().getName()+" release read lock.");
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testOneTime("exceed read with single thread", TestExceededRead0.class);
        Thread.sleep(1300);
        TestUtils.testWithThreadPool("exceed read with multi thread", TestExceededRead0.class);
    }
}
