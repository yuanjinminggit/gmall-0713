package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    private String keyword;
    private List<Long> brandId;
    private List<Long> categoryId;
    private List<String> props;
    private Double priceFrom;
    private Double priceTo;
    private Boolean store;
    private  Integer sort;
    private Integer pageNum = 1;
    private  final Integer pageSize = 10;

}
