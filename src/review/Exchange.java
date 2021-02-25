package review;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Exchanger;

/**
 * @Author: YHM
 * @Date: 2021/2/24 13:07
 */
public class Exchange {

    private static class Customer {
        int id;
        String name;

        public Customer(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Customer{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    private static class Product {
        int id;
        String name;
        int cost;

        public Product(int id, String name, int cost) {
            this.id = id;
            this.name = name;
            this.cost = cost;
        }

        @Override
        public String toString() {
            return "Product{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", cost=" + cost +
                    '}';
        }
    }

    private static Exchanger<Object> exchanger = new Exchanger<>();

    private static Random random = new Random(13);

    private static class Shop extends Thread {
        public Shop() {
            super();
        }

        @Override
        public void run() {
            int id = random.nextInt(131313);
            String name = "YHM";
            Customer customer = new Customer(id, name);
            try {
                Thread.sleep(1000);
                System.out.println("Exchange product with customer");
                System.out.println((Product) exchanger.exchange(customer));
            } catch (InterruptedException ignore) {}
        }
    }

    private static class Pay extends Thread {
        public Pay() {
            super();
        }

        @Override
        public void run() {
            int id = random.nextInt(131313);
            String name = "Uploader";
            // 白嫖
            int cost = 0;
            Product product = new Product(id, name, cost);
            try {
                System.out.println("Exchange customer with product");
                Thread.sleep(500);
                System.out.println((Customer) exchanger.exchange(product));
            } catch (InterruptedException ignore) {}
        }
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Shop();
        Thread t2 = new Pay();
        t1.start();t2.start();
        t1.join();t2.join();
    }
}
