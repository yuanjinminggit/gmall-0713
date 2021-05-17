package com.atguigu.gmall.pms.mapper;


import com.atguigu.gmall.pms.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryMapperTest {
@Autowired
    CategoryMapper categoryMapper;
@Autowired
CategoryService categoryService;
        @Test
    void t(){
    System.out.println(categoryMapper.queryCategoriesWithSubsByPid(1l));
}
    @Test
    void t1(){
    System.out.println(categoryService.queryCategoriesWithSubsByPid(1l));
}


}