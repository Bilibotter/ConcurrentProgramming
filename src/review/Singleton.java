package review;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: YHM
 * @Date: 2021/2/21 15:11
 */
public class Singleton {
    private final static AtomicReference<Singleton> REFERENCE = new AtomicReference<>();

    private Singleton() {
    }

    public Singleton getInstance() {
        for (;;) {
            Singleton instance = REFERENCE.get();
            if (instance != null) {
                return instance;
            }
            instance = new Singleton();
            if (REFERENCE.compareAndSet(null, instance)) {
                return instance;
            }
        }
    }
}
