package com.atguigu.demo.proxy2;


import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.io.JsonEOFException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

public class ProxyDemo implements InvocationHandler {
    private ZhangSan zhangSan;

    public ProxyDemo(ZhangSan zhangSan) {
        this.zhangSan = zhangSan;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("zengqiangqian ");
        Object result = method.invoke(zhangSan,args);
        System.out.println("增强后");
        return result;
    }
    public static void main(String[] args) {
        ZhangSan zhangSan = new ZhangSan("zhangsan");
        ProxyDemo proxyDemo = new ProxyDemo(zhangSan);
        Object o = Proxy.newProxyInstance(zhangSan.getClass().getClassLoader(), zhangSan.getClass().getInterfaces(), proxyDemo);
//        System.out.println(proxy.getName());
        Person o1 = (Person) o;
        o1.sayHello();
        ArrayList<ZhangSan> list = new ArrayList<>();
        list.add(new ZhangSan("lisi"));
        list.add(zhangSan);
        System.out.println(JSON.toJSONString(list));
        HashMap<String, String> map1 = new HashMap<>();
        map1.put("1","dhfuhdu");
        map1.put("2","fjidjfi");
        System.out.println(map1);
        HashMap<String, String> map2 = new HashMap<>();
        map2.put("1","dhfuhdu");
        map2.put("2","fjidjfi");
        System.out.println(JSON.toJSONString(map2));
        ArrayList<Object> list2 = new ArrayList<>();
        list2.add(map1);
        list2.add(map2);
        System.out.println(list2);




    }






}
