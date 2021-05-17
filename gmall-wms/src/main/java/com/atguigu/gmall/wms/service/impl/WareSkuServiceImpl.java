package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;
    @Resource
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String key_prefix="stock:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos,String orderToken) {

        if (CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("没有选中的商品,请去购物车选中要购买商品!");
        }
        lockVos.forEach(lockVo ->{
            checkLock(lockVo);
        });
        //是否都锁定成功,如果有一个锁定失败,所有锁定成功的都应该解锁库存
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())){
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(successLockVos)){
                successLockVos.forEach(lockVo -> {
                    this.wareSkuMapper.unLock(lockVo.getWareSkuId(),lockVo.getCount());
                });
            }
            //再有商品锁定失败情况下,把锁定信息展示给消费方
            return lockVos;

        }
        //所有商品锁定成功时,放到redis中,方便以后解锁库存
        this.redisTemplate.opsForValue().set(key_prefix+orderToken, JSON.toJSONString(lockVos));
        //所有商品锁定成功之后,发送消息定时解锁库存
        this.rabbitTemplate.convertAndSend("order_exchange","stock.ttl",orderToken);

        return null;
    }
    private void checkLock(SkuLockVo lockVo){
        RLock fairLock = this.redissonClient.getFairLock("stock:lock:" + lockVo.getSkuId());
        try {
            fairLock.lock();
            //盐库存   查询
            List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.check(lockVo.getSkuId(), lockVo.getCount());
            if (CollectionUtils.isEmpty(wareSkuEntities)){
                lockVo.setLock(false);
                return;
            }


            //锁库存  更新库存表中的stock_lock字段 区第一条库存
            Long id = wareSkuEntities.get(0).getId();
            if (this.wareSkuMapper.lock(id,lockVo.getCount())==1){
                lockVo.setLock(true);
                lockVo.setWareSkuId(id);

                return;
            }


        } finally {
            fairLock.unlock();
        }


    }

}