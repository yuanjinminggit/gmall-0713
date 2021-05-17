package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.wms.entity.PurchaseDetailEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-12-15 23:43:42
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

