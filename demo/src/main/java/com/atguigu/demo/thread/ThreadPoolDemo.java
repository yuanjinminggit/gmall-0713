package com.atguigu.demo.thread;

import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService poolExecutor = new ThreadPoolExecutor(2, 5, 2, TimeUnit.SECONDS, new ArrayBlockingQueue<>(3),
                Executors.defaultThreadFactory(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                System.out.println("自定义拒绝策略");
            }
        }
        );
        try {
            for (int i = 0; i < 9; i++) {
                poolExecutor.execute(()->{
                    System.out.println(Thread.currentThread().getName()+"执行了业务逻辑");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            poolExecutor.shutdown();
        }
        AtomicInteger i = new AtomicInteger(1);
        System.out.println(i.compareAndSet(1,200));
        System.out.println(i.incrementAndGet());


    }
}
