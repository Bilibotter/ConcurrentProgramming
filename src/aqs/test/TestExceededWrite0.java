package aqs.test;

import static aqs.test.GlobalInstance.WRITE0;

/**
 * @Author: YHM
 * @Date: 2021/2/25 11:15
 */
public class TestExceededWrite0 implements Runnable {
    @Override
    public void run() {
        for (int i=0;;i++) {
            try {
                WRITE0.lock();
            }catch (Error e) {
                e.printStackTrace();
                System.out.println("Success exceeded writer with lock time "+i);
                break;
            }
        }
        for (;;) {
            try {
                WRITE0.unlock();
            }catch (IllegalMonitorStateException ignore) {
                System.out.println("Release write lock.");
                break;
            }
        }
        try {
            Thread.sleep(1000);
        }catch (InterruptedException ignore) {}
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testOneTime("exceeded write lock", TestExceededWrite0.class);
    }
}
