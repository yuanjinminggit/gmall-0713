package com.atguigu.gmall.order.config;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;
@Slf4j
@Configuration
public class RabbitConfig {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData var1, boolean ack, @Nullable String cause)->{
            if (!ack){
                log.error("消息没有到达交换机.原因:{}",cause);
            }
        });
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey)->{
            log.error("消息没有到达队列.交换机:{},路右键:{},消息内容:{}",exchange,routingKey,new String(message.getBody()));
        });
    }
    //演示队列声明
    @Bean
    public Queue ttlQueue(){
        return QueueBuilder.durable("order_ttl_queue")
                .withArgument("x-message-ttl",90000)
                .withArgument("x-dead-letter-exchange","order_exchange")
                .withArgument("x-dead-letter-routing-key","order.dead")
                .build();
    }
    //延时队列绑定到交换机
    @Bean
    public Binding ttlBinding(){
        return new Binding("order_ttl_queue",Binding.DestinationType.QUEUE,"order_exchange","order.ttl",null);
    }
    //死信队列
    @Bean
    public Queue deadQueue(){
        return QueueBuilder.durable("order_dead_queue").build();
    }

    //绑定到死信交换机:order.dead
    @Bean
    public Binding deadBinding(){
        return new Binding("order_dead_queue",Binding.DestinationType.QUEUE,"order_exchange","order.dead",null);

    }



}
