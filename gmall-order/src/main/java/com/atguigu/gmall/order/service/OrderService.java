package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginIntercepter;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String key_prefix="order:token:";


    public OrderConfirmVo confirm() {
        //同过拦截器获取登录用户的id
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        Long userId = userInfo.getUserId();


        OrderConfirmVo confirmVo = new OrderConfirmVo();


        ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressesByUserId(userId);
        List<UserAddressEntity> userAddressEntities = addressResponseVo.getData();
        confirmVo.setAddresses(userAddressEntities);


        ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> carts = cartResponseVo.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new CartException("你没有选中的购物车记录");
        }
        List<OrderItemVo> items = carts.stream().map(cart -> {
            OrderItemVo itemVo = new OrderItemVo();
            //为了保证最新数据,仅仅从购物车中获取skuid和count
            itemVo.setSkuId(cart.getSkuId());
            itemVo.setCount(cart.getCount());


            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity!=null){
                itemVo.setTitle(skuEntity.getTitle());
                itemVo.setPrice(skuEntity.getPrice());
                itemVo.setWeight(skuEntity.getWeight());
                itemVo.setDefaultImage(skuEntity.getDefaultImage());

            }
            ResponseVo<List<ItemSaleVo>> itemSaleResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = itemSaleResponseVo.getData();
            itemVo.setSales(itemSaleVos);

            ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(skuAttrValueEntities);

            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
            }
            return itemVo;
        }).collect(Collectors.toList());

        confirmVo.setItems(items);


        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity!=null){
            confirmVo.setBounds(userEntity.getIntegration());

        }


        String orderToken = IdWorker.getTimeId();
        confirmVo.setOrderToken(orderToken);
        //为了防虫(提交订单的幂等性)
        this.redisTemplate.opsForValue().set(key_prefix+orderToken,orderToken);


        return confirmVo;
    }

    public OrderEntity submit(OrderSubmitVo submitVo) {
        //防虫(幂等性)
                //判断并删除
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("非法请求");
        }
        String script="if(redis.call('get',KEYS[1])==ARGV[1])then return redis.call('del',KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(key_prefix + orderToken),orderToken);
        if(!flag){
            throw new OrderException("请不要重复提交");
        }


        //眼总价
        List<OrderItemVo> items = submitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("请选择要购买的商品");
        }
        BigDecimal totalprice = submitVo.getTotalPrice();
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return new BigDecimal(0);

            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        if (totalprice.compareTo(currentTotalPrice)!=0){
            throw new OrderException("页面已过期,请刷新后再试");
        }



        //盐库存并锁定库存
        List<SkuLockVo>lockVos = items.stream().map(item->{
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockResponseVo = this.wmsClient.checkAndLock(lockVos, orderToken);
        List<SkuLockVo> skuLockVos = skuLockResponseVo.getData();
        //如果盐库存何所库存返回值不为空.说明盐库存何所库存失败,提示锁定信息
        if (!CollectionUtils.isEmpty(skuLockVos)){
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }




        //int i = 1/0;

        //创建订单
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        Long userId = userInfo.getUserId();
        OrderEntity orderEntity = null;
        try {
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo, userId);
            orderEntity = orderEntityResponseVo.getData();
            this.rabbitTemplate.convertAndSend("order_exchange","order.ttl",orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            // todo:解锁库存 标记为无效订单
            //不管什么异常,直接发送消息给oms更新订单状态,订单勋在更新为无效订单,订单不存在影响条数为0
            this.rabbitTemplate.convertAndSend("order_exchange","order.disable",orderToken);
            throw new OrderException("创建订单失败,请联系运营人员");


        }
        //删除购物车中对应记录
        Map<String,Object> map = new HashMap<>();
        map.put("userId",userInfo.getUserId());
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds",JSON.toJSONString(skuIds));

        this.rabbitTemplate.convertAndSend("order_exchange","cart.delete",map);
        return orderEntity;
    }
}
