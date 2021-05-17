package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCatgoriesByPid(Long pid) {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        if (pid != -1){
            wrapper.eq("parent_id",pid);

        }
        return this.list(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSubsByPid(Long pid) {
        //return this.queryCategoriesWithSubsByPid(pid);
        return this.baseMapper.queryCategoriesWithSubsByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryLvl123CategoriesByCid3(Long id) {
        CategoryEntity lv3lCategory = this.getById(id);
        if (lv3lCategory==null){
            return null;
        }

        CategoryEntity lvl2Category = this.getById(lv3lCategory.getParentId());
        CategoryEntity lvl1Category = this.getById(lvl2Category.getParentId());
        return Arrays.asList(lvl1Category,lvl2Category,lv3lCategory);
    }

}