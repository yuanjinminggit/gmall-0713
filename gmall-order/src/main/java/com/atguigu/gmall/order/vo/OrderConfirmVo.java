package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {
    private List<UserAddressEntity> addresses;
    //送货清单
    private List<OrderItemVo> items;
    //购物积分
    private Integer bounds;
    //防止重复提交唯一标识(保证幂等性)
    private String orderToken;


}
