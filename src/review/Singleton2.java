package review;

/**
 * @Author: YHM
 * @Date: 2021/2/21 15:26
 */
public class Singleton2 {
    private final static Singleton2 instance = new Singleton2();
    private Singleton2() {}
    public Singleton2 getInstance() {
        return instance;
    }
}
