package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.CouponHistoryEntity;
import com.atguigu.gmall.sms.mapper.CouponHistoryMapper;
import com.atguigu.gmall.sms.service.CouponHistoryService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;


@Service("couponHistoryService")
public class CouponHistoryServiceImpl extends ServiceImpl<CouponHistoryMapper, CouponHistoryEntity> implements CouponHistoryService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CouponHistoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CouponHistoryEntity>()
        );

        return new PageResultVo(page);
    }

}