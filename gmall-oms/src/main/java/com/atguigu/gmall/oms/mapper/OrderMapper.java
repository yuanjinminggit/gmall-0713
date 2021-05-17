package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.PrintWriter;

/**
 * 订单
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-01-08 23:22:46
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
   public int updateStatus(@Param("orderToken") String orderToken, @Param("target") Integer targetStatus, @Param("expect") Integer expectStatus);
	
}
