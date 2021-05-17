package com.atguigu.demo.proxy3;

public class CommonPerson implements BuyTicket {

    @Override
    public void buyTicket() {
        System.out.println("买到票了！");
    }

}