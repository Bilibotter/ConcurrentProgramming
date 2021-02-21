package review;

import java.util.concurrent.*;

/**
 * @Author: YHM
 * @Date: 2021/2/21 16:28
 */
public class ProducerConsumer {
    private final static int PRODUCT_NUM = 100;
    private final static CountDownLatch latch = new CountDownLatch(PRODUCT_NUM);
    private final static BlockingQueue<Product> queue = new LinkedBlockingQueue<>(13);
    private static class Product {
        private final double weight;
        Product() {
            weight = Math.random() * 100;
        }

        public double getWeight() {
            return weight;
        }
    }
    private static class Producer extends Thread {
        Producer() {
            super();
        }

        @Override
        public void run() {
            try {
                queue.put(new Product());
            } catch (InterruptedException ignore) {}
        }
    }
    private static class Consumer extends Thread {
        Consumer() {
            super();
        }

        @Override
        public void run() {
            try {
                System.out.println("Weight is "+queue.take().getWeight());
            }catch (InterruptedException ignore) {}
            latch.countDown();
        }
    }

    public static void main(String[] args) throws Exception {
        int producers = PRODUCT_NUM;
        int consumers = PRODUCT_NUM;
        while (producers > 0 || consumers > 0) {
            if (Math.random() > 0.5 && producers > 0) {
                new Producer().start();
                producers--;
            }else if (consumers > 0) {
                new Consumer().start();
                consumers--;
            }
        }
        latch.await();
        System.out.println("Finish!");
    }
}
