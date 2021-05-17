package com.atguigu.gmall.scheduled.jobHandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class MyJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private CartMapper cartMapper;
    private static final String key_prefix="cart:info:";

    private static final String exception_key="cart:exception:userId";
    @XxlJob("cartDataSyncJobHandler")
    public ReturnT<String> dataSync(String param){


        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(exception_key);
        String userId = setOps.pop();
        while (userId!=null){
            //全部删除失败用户的MySQL中的购物车
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId));
            //读取redis中失败用户的购物车记录,redis中没有直接结束
            if (!this.redisTemplate.hasKey(key_prefix+userId)){
                return ReturnT.SUCCESS;
            }
            //新增redis中对应的购物车记录
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key_prefix + userId);
            List<Object> cartJsons = hashOps.values();
            cartJsons.forEach(cartJson->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                this.cartMapper.insert(cart);
            });

            userId=setOps.pop();
        }


        return ReturnT.SUCCESS;
    }

    /*
    *1.方法必须有returnT<String>返回值，必须有一个String类型的形参
    *2.@xxljob注解声明该方法是一个定时任务
    *3.输出日志用xxljoblog.log输出日志
    *
    *
    * */
    @XxlJob("myJobHandler")
    public ReturnT<String> test(String param){
        System.out.println("任务执行时间"+System.currentTimeMillis()+param);
        XxlJobLogger.log("myjobhandler executed"+param);
        return ReturnT.SUCCESS;
    }



}
