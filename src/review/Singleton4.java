package review;

/**
 * @Author: YHM
 * @Date: 2021/2/21 17:24
 * DCL真是丑陋
 */
public class Singleton4 {
    private volatile static Singleton4 instance;

    private Singleton4() {}

    public static Singleton4 getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (Singleton4.class) {
            if (instance == null) {
                instance = new Singleton4();
            }
        }
        return instance;
    }
}
