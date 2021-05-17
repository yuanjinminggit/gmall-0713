package com.atguigu.demo.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {

    public static void main(String[] args) {
        ZhangSan zs =new ZhangSan();

        Person personProxy = (Person)Proxy.newProxyInstance(zs.getClass().getClassLoader(), zs.getClass().getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("欢迎光临");
                Object invoke = method.invoke(zs, args);
                System.out.println("房子已经找好");
                return invoke;
            }
        });
        personProxy.searchHouse();
    }
}
