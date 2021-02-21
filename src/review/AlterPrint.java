package review;

/**
 * @Author: YHM
 * @Date: 2021/2/21 17:45
 * 想到的第一种交替打印的解法
 */
public class AlterPrint {
    static volatile boolean flag = false;
    static class PrintDigit extends Thread {
        @Override
        public void run() {
            for (int i=1;i<=26;i++) {
                while (flag) {}
                System.out.println(i);
                flag = !flag;
            }
        }
    }
    static class PrintLetter extends Thread {
        @Override
        public void run() {
            for (int i=0;i<26;i++) {
                while (!flag) {}
                System.out.println((char)(65+i));
                flag = !flag;
            }
        }
    }

    public static void main(String[] args) {
        new PrintDigit().start();
        new PrintLetter().start();
    }
}
