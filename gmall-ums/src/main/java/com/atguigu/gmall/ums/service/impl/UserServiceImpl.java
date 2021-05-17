package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.util.CollectionUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch(type){
            case 1: wrapper.eq("username",data);break;
            case 2: wrapper.eq("phone",data);break;
            case 3: wrapper.eq("email",data);break;
            default:
                return null;
        }


        return  this.count(wrapper)==0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        //校验短信验证码
        //生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);
        //对明文密码进行加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+salt));
        //新增用户
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(0);
        userEntity.setCreateTime(new Date());


        this.save(userEntity);


    }

    @Override
    public UserEntity queryUser(String loginName, String passWord) {
        //根据登录名查询用户
        List<UserEntity> users = this.list(new QueryWrapper<UserEntity>().eq("userName", loginName).or().eq("email", loginName).or().eq("phone", loginName));

        //判断用户是否为空
        if (CollectionUtils.isEmpty(users)){
            return null;
        }

        for (UserEntity user : users) {
            //获取该用户的盐,并对用户输入的明文密码进行加密
            passWord= DigestUtils.md5Hex( passWord+user.getSalt());
            //比较
            if (StringUtils.equals(passWord,user.getPassword())){
                return user;
            }

        }
        return null;
    }

}