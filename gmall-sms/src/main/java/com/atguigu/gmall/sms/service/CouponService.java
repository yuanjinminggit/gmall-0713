package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 优惠券信息
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-12-14 23:50:47
 */
public interface CouponService extends IService<CouponEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

