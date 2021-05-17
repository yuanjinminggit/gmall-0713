package com.atguigu.demo.thread;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ShareDataTwo {
    private Integer flag =1;
    private final Lock lock =new ReentrantLock();
    private final Condition condition1 = lock.newCondition();
    private final Condition condition2 = lock.newCondition();
    private final Condition condition3 = lock.newCondition();
    public void print5(){
        lock.lock();
        try {
            while (flag!=1){
                condition1.await();

            }
            for (int i = 0; i < 5; i++) {

                System.out.println(Thread.currentThread().getName()+"\t"+(i+1));
            }
            flag =2;
            condition1.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
    public void print10(){
        lock.lock();
        try {
            while (flag!=2){
                condition1.await();
            }
            for (int i = 0; i < 10; i++) {

                System.out.println(Thread.currentThread().getName()+"\t"+(i+1));
            }
            flag=3;
            condition1.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
    public void print15(){
        lock.lock();
        try {
            while(flag!=3){
                condition1.await();
            }
            for (int i = 0; i < 15; i++) {
                System.out.println(Thread.currentThread().getName()+"\t"+(i+1));
            }
            flag=1;
            condition1.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


}



