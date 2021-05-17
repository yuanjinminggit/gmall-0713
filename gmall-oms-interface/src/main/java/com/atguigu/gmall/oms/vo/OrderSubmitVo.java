package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {
    private String orderToken;
    //页面提交下单时的总价格
    private BigDecimal totalPrice;

    private List<OrderItemVo>items;
    //支付方式
    private Integer payType;
    //配送方式物流公司
    private String deliveryCompany;

    private UserAddressEntity address;

    private Integer bounds;

}
