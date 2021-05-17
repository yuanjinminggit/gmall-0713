package com.atguigu.demo.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Demo {
    public static void main(String[] args) {
        List<String> arrayList1 = new ArrayList<String>();
        arrayList1.add("1");
        arrayList1.add("2");
        for (String s : arrayList1) {
            if("1".equals(s)){
                arrayList1.remove(s);
            }}
        List<String> arrayList2 = new ArrayList<String>();
        arrayList2.add("2");
        arrayList2.add("1");
        for (String s : arrayList2) {
            if("1".equals(s)){
                arrayList2.remove(s);
            }
        }
//        Map.Entry<String,String>
//        ThreadLocal
//        Thread
        HashMap<Object, Object> map = new HashMap<>();
//        map.keySet()


    }
}
