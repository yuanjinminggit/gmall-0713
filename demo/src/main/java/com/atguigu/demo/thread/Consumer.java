package com.atguigu.demo.thread;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Buffer{
    private  final Lock lock;
    private final Condition notFull;
    private final Condition notEmpty;
    private int maxSize;
    private List<Date> storage;
    Buffer(int size){

        lock = new ReentrantLock();
        notFull=lock.newCondition();
        notEmpty=lock.newCondition();
        maxSize=size;
        storage=new LinkedList<>();
    }
    public void put(){
        try {
            lock.lock();
            while (storage.size()==maxSize){
                System.out.println(Thread.currentThread().getName()+":wait \n");
                notFull.await();
            }
            storage.add(new Date());
            System.out.println(Thread.currentThread().getName()+"put:"+storage.size()+"\n");
            Thread.sleep(1000);
            notEmpty.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
    public void take(){
        try {
            lock.lock();
            while(storage.size()==0){
                System.out.println(Thread.currentThread().getName()+"wait");
                notEmpty.await();
            }
            Date d = ((LinkedList<Date>) storage).poll();
            System.out.println(Thread.currentThread().getName()+"take"+storage.size());
            notFull.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }


}
public class Consumer implements Runnable {
    private Buffer buffer;
    Consumer(Buffer buffer){
        this.buffer=buffer;
    }
    @Override
    public void run() {
        while(true){
            buffer.take();
        }


    }
}
class Producer implements Runnable{
    private Buffer buffer;
    Producer(Buffer b){
        buffer=b;
    }

    @Override
    public void run() {
        while (true){
            buffer.put();
        }

    }
}
class Main{
    public static void main(String[] args) {
        Buffer buffer = new Buffer(10);
        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);
        for (int i = 0; i < 3; i++) {
            new Thread(producer,"producer"+i).start();
            
        }
        for (int i = 0; i < 3; i++) {
            new Thread(consumer,"consumer"+i).start();

        }
    }
}