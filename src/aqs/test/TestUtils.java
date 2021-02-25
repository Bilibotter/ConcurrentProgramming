package aqs.test;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: YHM
 * @Date: 2021/2/25 10:01
 */
public class TestUtils {
    protected static ThreadPoolExecutor getPool() {
        int processor = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(processor*2, processor*5, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(processor*100), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        return exec;
    }
    
    protected static void testOneTime(Class<?>... clss) throws Exception {
        testOneTime("task", clss);
    }
    
    protected static void testOneTime(String name, Class<?>... clss) throws Exception {
        Thread[] threads = new Thread[clss.length];
        long start = System.currentTimeMillis();
        for (int i=0; i<clss.length; i++) {
            threads[i] = new Thread((Runnable) clss[i].newInstance());
            threads[i].start();
        }
        for (Thread thread:threads) {
            thread.join();
        }
        System.out.println("Cost time is "+(System.currentTimeMillis()-start));
        System.out.println("Finish "+name+"!");
    }

    protected static void testWithThreadPool(Class<?>... clss) throws Exception {
        testWithThreadPool("task", clss);
    }

    protected static void testWithThreadPool(String name, Class<?>... clss) throws Exception {
        int processor = Runtime.getRuntime().availableProcessors();
        int execTime = processor * 100;
        ThreadPoolExecutor exec = getPool();
        long start = System.currentTimeMillis();
        for (int i=0; i<execTime; i++) {
            for (Class<?> cls:clss) {
                exec.execute((Runnable) cls.newInstance());
            }
        }
        exec.shutdown();
        while (!exec.isTerminated());
        System.out.println("Cost time is "+(System.currentTimeMillis()-start));
        System.out.println("Finish "+name+"!");
    }
}
