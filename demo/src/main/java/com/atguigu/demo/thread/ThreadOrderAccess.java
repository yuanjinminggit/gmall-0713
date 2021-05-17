package com.atguigu.demo.thread;

public class ThreadOrderAccess{
    public static void main(String[] args) {
        ShareDataTwo shareDataTwo = new ShareDataTwo();
        new Thread(()->{
            for (int i = 0; i < 10; i++) {
                shareDataTwo.print5();
            }
        },"AAA").start();

        new Thread(()->{
            for (int i = 0; i < 10; i++) {
                shareDataTwo.print10();
            }
        },"BBB").start();

        new Thread(()->{
            for (int i = 0; i < 10; i++) {
                shareDataTwo.print15();
            }
        },"CCC").start();
    }

}
