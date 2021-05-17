package com.atguigu.demo.demo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class PersonProxy implements InvocationHandler {
    private ZhangSan zhangSan;
    public PersonProxy(ZhangSan zhangSan){
        this.zhangSan = zhangSan;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("前置增强");
        Object result = method.invoke(zhangSan, args);
        System.out.println("搜之曾庆");
        return result ;
    }

    public static void main(String[] args) {
        ZhangSan zhangSan = new ZhangSan();
        PersonProxy personProxy = new PersonProxy(zhangSan);
        Person o = (Person)Proxy.newProxyInstance(zhangSan.getClass().getClassLoader(), zhangSan.getClass().getInterfaces(), personProxy);
        o.sayHello();

    }
}
