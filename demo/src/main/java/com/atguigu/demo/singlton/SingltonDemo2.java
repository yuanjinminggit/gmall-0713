package com.atguigu.demo.singlton;

public class SingltonDemo2 {
    /*
    * 在当前容器中   单个实例
    * 规则
    * 1,构造器私有化
    * 2,对外提供一个公共的静态的返回本类实例对象的方法
    * 分类
    * 懒汉式
    * 额韩式*/


    private SingltonDemo2(){}
    private static  SingltonDemo2 singltonDemo2=null;
    public synchronized static  SingltonDemo2 getInstance(){
            if (singltonDemo2==null){
                singltonDemo2=new SingltonDemo2();
            }
        return singltonDemo2;
    }

}
