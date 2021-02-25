package aqs.test;

import static aqs.test.GlobalInstance.WRITE;

/**
 * @Author: YHM
 * @Date: 2021/2/25 11:11
 */
public class TestExceededWrite implements Runnable {
    @Override
    public void run() {
        for (int i=0;;i++) {
            try {
                WRITE.lock();
            }catch (Error e) {
                e.printStackTrace();
                System.out.println("Success exceeded writer with lock time "+i);
                break;
            }
        }
        for (;;) {
            try {
                WRITE.unlock();
            }catch (Error ignore) {
                System.out.println("Release write lock.");
                break;
            }
        }
        try {
            Thread.sleep(1000);
        }catch (InterruptedException ignore) {}
    }

    public static void main(String[] args) throws Exception {
        TestUtils.testOneTime("exceeded write lock", TestExceededWrite.class);
    }
}
