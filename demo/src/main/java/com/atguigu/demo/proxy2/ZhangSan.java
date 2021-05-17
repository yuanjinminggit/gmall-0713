package com.atguigu.demo.proxy2;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class ZhangSan implements Person {
    private String name;
    @Override
    public void sayHello() {
        System.out.println("hello");
    }
    public void add(int a ,int b){

    }

}
