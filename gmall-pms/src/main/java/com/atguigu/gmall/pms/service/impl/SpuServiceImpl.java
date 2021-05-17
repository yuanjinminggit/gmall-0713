package com.atguigu.gmall.pms.service.impl;

import com.alibaba.nacos.client.utils.StringUtils;
import com.atguigu.gmall.pms.feign.GmallPmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;

import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;

import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Resource
    private SpuDescMapper spuDescMapper;
    @Resource
    private SpuAttrValueService attrValueService;
    @Resource
    private SkuMapper skuMapper;
    @Resource
    private SkuImagesService skuImagesService;
    @Resource
    private SkuAttrValueService skuAttrValueService;
    @Resource
    private GmallPmsClient pmsClient;
    @Resource
    private SpuDescService spuDescService;
    @Resource
    private RabbitTemplate rabbitTemplate;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(Long cid, PageParamVo paramVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        if (cid!=0){
            wrapper.eq("category_id",cid);
        }
        String key = paramVo.getKey();
        if (StringUtils.isNotBlank(key)){
            //wrapper.eq("id",key).or().like("name",key);
            wrapper.and(t->t.like("id",key).or().like("name",key));

        }
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                wrapper
        );



        return new PageResultVo(page);
    }
    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {

        Long spuId = saveSpu(spu);


        //saveSpuDesc(spu, spuId);
        this.spuDescService.saveSpuDesc(spu,spuId);
//        int a  = 1/0;
//        try {
//            TimeUnit.SECONDS.sleep(4);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        try {
//            new FileInputStream("vvvvvv");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }


        saveBaseAttr(spu, spuId);


        saveSkuInfo(spu, spuId);
        //int i = 1/0;
        this.rabbitTemplate.convertAndSend("pms_item_exchange","item.insert",spuId);



    }

    private Long saveSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }

    private void saveSkuInfo(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;
        }
        skus.forEach(sku->{
            sku.setSpuId(spuId);
            sku.setBrandId(spu.getBrandId());

            sku.setCatagoryId(spu.getCategoryId());
            List<String> images = sku.getImages();
            if (!CollectionUtils.isEmpty(images)){
                sku.setDefaultImage(StringUtils.isNotBlank(sku.getDefaultImage())? sku.getDefaultImage():images.get(0));
            }
            this.skuMapper.insert(sku);
            Long skuId = sku.getId();






            if (!CollectionUtils.isEmpty(images)){
                skuImagesService.saveBatch(images.stream().map(image->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(sku.getDefaultImage(),image)?1:0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));

            }


            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuAttrValueEntity -> skuAttrValueEntity.setSkuId(skuId));
                this.skuAttrValueService.saveBatch(saleAttrs);
            }


            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(sku,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.pmsClient.saveSales(skuSaleVo);

        });
    }

    private void saveBaseAttr(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            this.attrValueService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo,spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);

                return spuAttrValueEntity;


            }).collect(Collectors.toList()));
        }
    }
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void saveSpuDesc(SpuVo spu, Long spuId) {
//        List<String> spuImages = spu.getSpuImages();
//        if(!CollectionUtils.isEmpty(spuImages)){
//            SpuDescEntity spuDescEntity = new SpuDescEntity();
//            spuDescEntity.setSpuId(spuId);
//            spuDescEntity.setDecript(StringUtils.join(spuImages,","));
//            this.spuDescMapper.insert(spuDescEntity);
//
//        }
//    }
}

//    public static void main(String[] args) {
//        List<User> users = Arrays.asList(
//                new User(11L, "李艳", 20, 0),
//                new User(21L, "马蓉", 20, 0),
//                new User(31L, "小鹿", 20, 0),
//                new User(41L, "销量", 20, 0),
//                new User(51L, "老王", 20, 0)
//        );
//
//        users.stream().filter(user -> user.getSex()==0).collect(Collectors.toList()).forEach(user -> System.out.println(user));
//        System.out.println(users.stream().map(user -> user.getName()).collect(Collectors.toList()));
//        System.out.println(users.stream().map(user -> {
//            Person person = new Person();
//            person.setAge(user.getAge());
//            person.setUsername(user.getName());
//            return person;
//        }).collect(Collectors.toList()));
//        System.out.println(users.stream().map(User::getAge).reduce((a, b) -> a + b).get());
//
//    }
//
//}
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//@ToString
//class  User{
//    private Long id;
//    private String name;
//    private Integer age;
//    private Integer sex;
//}
//@Data
//@ToString
//class Person{
//    private String username;
//    private Integer age;
//}