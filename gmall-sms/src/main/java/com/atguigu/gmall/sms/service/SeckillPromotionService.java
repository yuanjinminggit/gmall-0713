package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.entity.SeckillPromotionEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 秒杀活动
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-12-14 23:50:47
 */
public interface SeckillPromotionService extends IService<SeckillPromotionEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

