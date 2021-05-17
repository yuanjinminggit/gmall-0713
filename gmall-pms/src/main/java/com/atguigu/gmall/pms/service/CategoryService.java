package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品三级分类
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-12-14 20:58:35
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<CategoryEntity> queryCatgoriesByPid(Long pid);

    List<CategoryEntity> queryCategoriesWithSubsByPid(Long pid);

    List<CategoryEntity> queryLvl123CategoriesByCid3(Long id);
}

