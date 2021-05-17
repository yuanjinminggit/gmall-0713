package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause)->{
                if (!ack){
                    log.error("消息没有到达交换机,原因:{}",cause);
                }
        });
        this.rabbitTemplate.setReturnCallback((Message message, int replycode, String replyText, String exchange, String routingkey)->{
                log.error("消息没有到达队列,交换机:{},路右键:{},消息内容:{}",exchange,routingkey,new String(message.getBody()));
        });
    }
}
