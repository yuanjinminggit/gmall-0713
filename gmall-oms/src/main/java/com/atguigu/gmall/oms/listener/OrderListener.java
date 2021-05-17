package com.atguigu.gmall.oms.listener;


import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;


@Component
public class OrderListener {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Resource
    private OrderMapper orderMapper;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "oms_disable_queue",durable = "true"),
            exchange = @Exchange(value = "order_exchange", ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.disable"}
    ))
    public void disableOrder(String orderToken, Message message, Channel channel) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
        //更新订单状态为无效订单
        this.orderMapper.updateStatus(orderToken,5,0);
        //发送消息给wms解锁库存
        this.rabbitTemplate.convertAndSend("order_exchange","stock.unlock",orderToken);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
    @RabbitListener(queues = "order_dead_queue")
    public void closeOrder(String orderToken, Message message, Channel channel) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
        //更新订单状态为关闭订单
        if (this.orderMapper.updateStatus(orderToken,4,0)==1) {
            this.rabbitTemplate.convertAndSend("order_exchange","stock.unlock",orderToken);

        }
        //发送消息给wms解锁库存
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "oms_success_queue",durable = "true"),
            exchange = @Exchange(value = "order_exchange", ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.success"}
    ))
    public void successOrder(String orderToken, Message message, Channel channel) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        //更新订单状态为无效订单
        if (this.orderMapper.updateStatus(orderToken,1,0)==1) {
            this.rabbitTemplate.convertAndSend("order_exchange","stock.minus",orderToken);
            //发送消息给用户来家积分

        }
        //发送消息给wms解锁库存
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}
