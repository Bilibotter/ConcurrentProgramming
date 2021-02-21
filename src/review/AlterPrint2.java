package review;

/**
 * @Author: YHM
 * @Date: 2021/2/21 17:55
 * 有点丑陋
 */

public class AlterPrint2 {
    private final static Object lock = new Object();
    static class PrintLetter extends Thread {
        @Override
        public void run() {
            for (int i = 1; i<=26; i++) {
                synchronized (lock) {
                    System.out.println(i);
                    lock.notify();
                }
                if (i==26) {
                    break;
                }
                synchronized (lock) {
                    try {
                        lock.wait();
                    }catch (InterruptedException ignore) {}
                }
            }
        }
    }

    static class PrintDigit extends Thread {
        @Override
        public void run() {
            for (int i = 0; i<26; i++) {
                synchronized (lock) {
                    System.out.println((char)(65+i));
                    lock.notify();
                }
                if (i == 25) {
                    break;
                }
                synchronized (lock) {
                    try {
                        lock.wait();
                    }catch (InterruptedException ignore) {}
                }
            }
        }
    }

    public static void main(String[] args) {
        new PrintDigit().start();
        new PrintLetter().start();
    }
}
