package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CartListener {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    private static final String key_prefix="cart:info:";


    private static final String price_prefix="cart:price:";
    @Resource
    private CartMapper cartMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart-price-queue",durable = "true"),
            exchange = @Exchange(value = "pms_item_exchange",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key={"item.update"}
    ))
    public void listener(Long spuId, Channel channel ,Message message) throws IOException {
        ResponseVo<List<SkuEntity>> listResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = listResponseVo.getData();
        skuEntities.forEach(skuEntity -> {
            if (this.redisTemplate.hasKey(price_prefix+skuEntity.getId()))
            this.redisTemplate.opsForValue().set(price_prefix+skuEntity.getId(),skuEntity.getPrice().toString());
        });
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart_delete_queue",durable = "true"),
            exchange = @Exchange(value = "order_exchange",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void deleteCart(Map<String,Object>map,Message message,Channel channel) throws IOException {
        if (CollectionUtils.isEmpty(map)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        String userId = map.get("userId").toString();
        String skuIdJson = map.get("skuIds").toString();

        List<String> skuIds = JSON.parseArray(skuIdJson, String.class);
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key_prefix + userId);
        hashOps.delete(skuIds.toArray());

        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId).in("sku_id",skuIds));
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

}
