package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;


    @GetMapping("pay.html")
    public String pay(@RequestParam("orderToken")String orderToken, Model model){
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity==null||orderEntity.getStatus()!=0){
            throw new OrderException("订单状态异常");
        }
        model.addAttribute("orderEntity",orderEntity);
        return "pay";
    }
    @GetMapping("alipay.html")
    @ResponseBody
    public String  toAlipay(@RequestParam("orderToken")String orderToken){
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity==null||orderEntity.getStatus()!=0){
            throw new OrderException("这个订单不属于您,或者订单状态异常");
        }
        //调用支付宝的接口,跳转到支付宝支付页面
        try {

            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);
            payVo.setTotal_amount("0.01");
            payVo.setSubject("鼓勵商城支付平台");
            //生成对账记录
            String payId = this.paymentService.savePayment(payVo);
            //把对账记录的ID,放入passback_params参数,支付成功后,异步毁掉时,会返回
            payVo.setPassback_params(payId);

            return this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }
    //同步
    @GetMapping("pay/success")
    public String paySuccess(){
        //获取订单编号,根据订单编号查询订单,获取金额
        return "paySuccess";
    }

    //异步
    @ResponseBody
    @PostMapping("pay/ok")
    public Object payOk(PayAsyncVo payAsyncVo){
        System.out.println("异步回掉接口:xxxxxxxx");

        //验签
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        //校验业务参数,平台,订单,金额
        //获取相应信息中的参数
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        //平台参数
        String cur_app_id = alipayTemplate.getApp_id();
        String payId = payAsyncVo.getPassback_params();
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryPayInfoById(payId);

        if (paymentInfoEntity==null||!StringUtils.equals(app_id,cur_app_id)||!StringUtils.equals(out_trade_no,paymentInfoEntity.getOutTradeNo())
        ||new BigDecimal(total_amount).compareTo(paymentInfoEntity.getTotalAmount())!=0){
            return "failure";
        }

        //校验支付状态table_success
        String trade_status = payAsyncVo.getTrade_status();
        if (!StringUtils.equals(trade_status,"TRADE_SUCCESS")){
            return "failure";
        }
        //通过后开始修改
        //修改对账表
        if (this.paymentService.updatePayInfo(payAsyncVo)) {
            //修改订单状态
            this.rabbitTemplate.convertAndSend("order_exchange","order.success",payAsyncVo.getOut_trade_no());


        }

        //返回success,失败返回failure
        return "success";
    }

}
