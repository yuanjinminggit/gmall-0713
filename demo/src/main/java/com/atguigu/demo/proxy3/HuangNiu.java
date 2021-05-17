package com.atguigu.demo.proxy3;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HuangNiu implements InvocationHandler {

    private final CommonPerson target;

    public HuangNiu(CommonPerson target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("黄牛帮忙代购");
        Object res = method.invoke(target, args);
        return res;
    }
    public static void main(String[] args) {
        //需要被代理的类
        CommonPerson commonPerson = new CommonPerson();

        //代理类
        HuangNiu huangNiu = new HuangNiu(commonPerson);

        //生成代理对象
        BuyTicket buyTicket = (BuyTicket) Proxy.newProxyInstance(CommonPerson.class.getClassLoader(), new Class[]{BuyTicket.class}, huangNiu);

        //调用代理对象的方法
        buyTicket.buyTicket();

    }
}