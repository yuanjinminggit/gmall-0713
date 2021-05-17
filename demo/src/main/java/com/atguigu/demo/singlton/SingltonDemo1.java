package com.atguigu.demo.singlton;

public class SingltonDemo1 {
    /*
    * 在当前容器中   单个实例
    * 规则
    * 1,构造器私有化
    * 2,对外提供一个公共的静态的返回本类实例对象的方法
    * 分类
    * 懒汉式
    * 额韩式*/


    private SingltonDemo1(){}
    private static final SingltonDemo1 singltonDemo1=new SingltonDemo1();
    public static SingltonDemo1 getInstance(){
        return singltonDemo1;
    }

}
