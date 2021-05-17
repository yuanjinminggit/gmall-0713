package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Resource
    private OrderItemMapper orderItemMapper;
    @Autowired
    private GmallPmsClient pmsClient;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {
        //保存订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice());
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());

        UserAddressEntity address = submitVo.getAddress();
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverAddress(address.getAddress());

        orderEntity.setDeleteStatus(0);

        this.save(orderEntity);
        Long orderEntityId = orderEntity.getId();


        //保存订单详情
        List<OrderItemVo> items = submitVo.getItems();
        items.forEach(item->{
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrderId(orderEntityId);
            itemEntity.setOrderSn(submitVo.getOrderToken());
            //根据skuid查询sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            itemEntity.setSkuId(skuEntity.getId());
            itemEntity.setSkuPic(skuEntity.getDefaultImage());
            itemEntity.setSkuName(skuEntity.getName());
            itemEntity.setSkuPrice(skuEntity.getPrice());
            itemEntity.setCategoryId(skuEntity.getCatagoryId());
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            itemEntity.setSpuBrand(brandEntity.getName());
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            itemEntity.setSpuId(spuEntity.getId());
            itemEntity.setSpuName(spuEntity.getName());
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();

            itemEntity.setSpuPic(spuDescEntity.getDecript());
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueVoResponseVo = this.pmsClient.querySaleAttrValueBySkuId(item.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueVoResponseVo.getData();
            itemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));



            this.orderItemMapper.insert(itemEntity);
        });

        //int i = 1/0;
        return orderEntity;
    }

}