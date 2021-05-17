package com.atguigu.gmall.payment.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.interceptor.LoginIntercepter;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import com.atguigu.gmall.payment.pojo.UserInfo;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {
    @Autowired
    private GmallOmsClient omsClient;
    @Resource
    private PaymentInfoMapper paymentInfoMapper;
    public OrderEntity queryOrder(String orderToken) {
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByUserIdAndOrderToken(orderToken, userInfo.getUserId());
        OrderEntity orderEntity = orderEntityResponseVo.getData();
        return orderEntity;
    }

    public String savePayment(PayVo payVo) {

        // 否则，新增支付记录
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setOutTradeNo(payVo.getOut_trade_no());
        paymentInfoEntity.setPaymentType(1);
        paymentInfoEntity.setSubject("谷粒商城支付平台");
        // paymentInfoEntity.setTotalAmount(orderEntity.getPayAmount());
        paymentInfoEntity.setTotalAmount(new BigDecimal(payVo.getTotal_amount()));
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setCreateTime(new Date());
        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId().toString();

    }
    public PaymentInfoEntity queryPayInfoById(String payId){
        return this.paymentInfoMapper.selectById(payId);
    }
    @Transactional
    public Boolean updatePayInfo(PayAsyncVo payAsyncVo){
        PaymentInfoEntity paymentInfoEntity = this.queryPayInfoById(payAsyncVo.getPassback_params());
        if (paymentInfoEntity.getPaymentStatus()==1) {
            return false;
        }
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        return this.paymentInfoMapper.updateById(paymentInfoEntity)==1;
    }
}
