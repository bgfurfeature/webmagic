package us.codecraft.webmagic.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread pool for workers.<br><br>
 * Use {@link java.util.concurrent.ExecutorService} as inner implement. <br><br>
 * New feature: <br><br>
 * 1. Block when thread pool is full to avoid poll many urls without process. <br><br>
 * 2. Count of thread alive for monitor.
 *
 * @author code4crafer@gmail.com
 * @since 0.5.0
 */
public class CountableThreadPool {

    private int threadNum;

    private AtomicInteger threadAlive = new AtomicInteger();

    // 线程同步锁
    private ReentrantLock reentrantLock = new ReentrantLock();

    private Condition condition = reentrantLock.newCondition();

    public CountableThreadPool(int threadNum) {
        this.threadNum = threadNum;
        this.executorService = Executors.newFixedThreadPool(threadNum);
    }
    // 自定义计数线程池
    public CountableThreadPool(int threadNum, ExecutorService executorService) {
        this.threadNum = threadNum;
        this.executorService = executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public int getThreadAlive() {
        return threadAlive.get();
    }

    public int getThreadNum() {
        return threadNum;
    }

    private ExecutorService executorService;

    public void execute(final Runnable runnable) {


        if (threadAlive.get() >= threadNum) {
            try {
                // 上锁
                reentrantLock.lock();
                while (threadAlive.get() >= threadNum) {
                    try {
                        // 等待
                        condition.await();
                    } catch (InterruptedException e) {
                    }
                }
            } finally {
                // 释放锁
                reentrantLock.unlock();
            }
        }
        // 活跃线程数增加
        threadAlive.incrementAndGet();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 线程运行
                    runnable.run();
                } finally {
                    try {
                        // 线程运行完修正活跃线程数
                        reentrantLock.lock();
                        threadAlive.decrementAndGet();
                        condition.signal();
                    } finally {
                        reentrantLock.unlock();
                    }
                }
            }
        });
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public void shutdown() {
        executorService.shutdown();
    }


}
