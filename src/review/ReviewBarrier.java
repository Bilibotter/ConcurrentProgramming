package review;

import java.util.concurrent.*;

/**
 * @Author: YHM
 * @Date: 2021/2/22 18:48
 */
public class ReviewBarrier {
    private static int processors = Runtime.getRuntime().availableProcessors();
    private static final CyclicBarrier barrier = new CyclicBarrier(processors, new Hint());
    private static class Hint implements Runnable {
        @Override
        public void run() {
            System.out.println(processors+" tasks is release.");
        }
    }
    private static class Rush extends Thread {
        public Rush() {
            super();
        }

        @Override
        public void run() {
            System.out.println(this.getName()+ " is rush to barrier!");
            try {
                barrier.await();
            } catch (Exception ignore) {}
            System.out.println(this.getName()+ " pass through barrier.");
        }
    }

    public static void main(String[] args) {
        ThreadPoolExecutor exec =
                new ThreadPoolExecutor(processors, processors*2, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(processors*20), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        int tasks = processors * 20;
        while (tasks-- != 0) {
            exec.execute(new Rush());
        }
        exec.shutdown();
        while (exec.isTerminated()) {}
        System.out.println("Finish!");
    }
}
