package aqs.test;

import aqs.DowngradeReadWriteLock;
import aqs.UpgradeReadWriteLock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: YHM
 * @Date: 2021/2/25 10:48
 */
public class GlobalInstance {
    protected final static UpgradeReadWriteLock LOCK = new UpgradeReadWriteLock(false);

    protected final static UpgradeReadWriteLock.ReadLock READ = LOCK.readerLock();

    protected final static UpgradeReadWriteLock.WriteLock WRITE = LOCK.writerLock();

    protected final static DowngradeReadWriteLock LOCK0 = new DowngradeReadWriteLock();

    protected final static DowngradeReadWriteLock.ReadLock READ0 = LOCK0.readerLock();

    protected final static DowngradeReadWriteLock.WriteLock WRITE0 = LOCK0.writerLock();

    protected final static ReentrantReadWriteLock LOCK1 = new ReentrantReadWriteLock();

    protected final static ReentrantReadWriteLock.ReadLock READ1 = LOCK1.readLock();

    protected final static ReentrantReadWriteLock.WriteLock WRITE1 = LOCK1.writeLock();
}
