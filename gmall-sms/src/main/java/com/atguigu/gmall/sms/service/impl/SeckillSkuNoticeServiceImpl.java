package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SeckillSkuNoticeEntity;
import com.atguigu.gmall.sms.mapper.SeckillSkuNoticeMapper;
import com.atguigu.gmall.sms.service.SeckillSkuNoticeService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;


@Service("seckillSkuNoticeService")
public class SeckillSkuNoticeServiceImpl extends ServiceImpl<SeckillSkuNoticeMapper, SeckillSkuNoticeEntity> implements SeckillSkuNoticeService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SeckillSkuNoticeEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SeckillSkuNoticeEntity>()
        );

        return new PageResultVo(page);
    }

}