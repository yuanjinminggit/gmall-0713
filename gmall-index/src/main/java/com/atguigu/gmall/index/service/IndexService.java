package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.tools.DistributeLock;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    private static final String  key_prefix ="index:cates";
    @Autowired
    private DistributeLock distributeLock;
    @Autowired
    private RedissonClient redissonClient;
    public List<CategoryEntity> queryLv1CategoriesByPid(){

        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.queryCatgoriesByPid(0L);
        return responseVo.getData();

    }


    @GmallCache(prefix = key_prefix,timeout = 43200,random = 4320 ,lock = "lock:index:")
    public List<CategoryEntity> queryLv12CategoriesWithSubByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubsByPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();
        return data;

    }



    public List<CategoryEntity> queryLv12CategoriesWithSubByPid2(Long pid) {
        //1.先查询缓存
        String json = this.redisTemplate.opsForValue().get(key_prefix+pid);
        if (!StringUtils.isBlank(json)&&!StringUtils.equals("null",json)){
            return JSON.parseArray(json,CategoryEntity.class);
        }else if (StringUtils.equals("null",json)){
            return null;
        }
        RLock lock = this.redissonClient.getLock("index:lock:" + pid);
        lock.lock();

        String json2 = this.redisTemplate.opsForValue().get(key_prefix+pid);
        if (!StringUtils.isBlank(json2)&&!StringUtils.equals("null",json2)){
            return JSON.parseArray(json2,CategoryEntity.class);
        }else if (StringUtils.equals("null",json2)){
            return null;
        }





        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubsByPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();
        //放入缓存
        if (!CollectionUtils.isEmpty(data)){
            this.redisTemplate.opsForValue().set(key_prefix+pid,null,3, TimeUnit.MINUTES);


        }else{
            this.redisTemplate.opsForValue().set(key_prefix+pid,JSON.toJSONString(data),30+new Random().nextInt(10), TimeUnit.DAYS);


        }


        return data;
    }

    public void testLock3() {
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.distributeLock.tryLock("lock", uuid, 30);
        if (flag){
            String number = this.redisTemplate.opsForValue().get("number");
            if(StringUtils.isBlank(number)) {
                return;
            }
            int num = Integer.parseInt(number);
            this.redisTemplate.opsForValue().set("number",String.valueOf(++num));
            //this.testSubLock(uuid);
//            try {
//                TimeUnit.SECONDS.sleep(90);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            this.distributeLock.unlock("lock",uuid);

        }

    }
    public void testSubLock(String uuid){
        this.distributeLock.tryLock("lock",uuid,30);
        System.out.println("测试可重入锁");
        this.distributeLock.unlock("lock",uuid);

    }







    public void testLock2() {
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
        if (!flag){
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }else{
//            this.redisTemplate.expire("lock",3,TimeUnit.SECONDS);
            String number = this.redisTemplate.opsForValue().get("number");
            if (StringUtils.isBlank(number)){
                return;
            }
            int num = Integer.parseInt(number);
            this.redisTemplate.opsForValue().set("number",String.valueOf(++num));
//            if (StringUtils.equals(uuid,redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//            }
            String script="if(redis.call('get',KEYS[1])==ARGV[1])then return redis.call('del',KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"),uuid);



        }



    }


    public void testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();

        String number = this.redisTemplate.opsForValue().get("number");
            if(StringUtils.isBlank(number)) {
                return;
            }
            int num = Integer.parseInt(number);
            this.redisTemplate.opsForValue().set("number",String.valueOf(++num));
        try {
            TimeUnit.SECONDS.sleep(90);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //this.testSubLock(uuid);
        lock.unlock();

    }


    public void testRead() {
        RReadWriteLock rwlock = this.redissonClient.getReadWriteLock("rwlock");
        rwlock.readLock().lock(10,TimeUnit.SECONDS);
        System.out.println("==============");

    }

    public void testWrite() {
        RReadWriteLock rwlock = this.redissonClient.getReadWriteLock("rwlock");
        rwlock.writeLock().lock(10,TimeUnit.SECONDS);
        System.out.println("====================================");


    }

    public void testCountdown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.countDown();

    }

    public void testLatch() {

        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
