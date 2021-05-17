package com.atguigu.gmall.order.interceptor;


import com.atguigu.gmall.cart.pojo.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component

public class LoginIntercepter implements HandlerInterceptor {


    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

//    public static String userId;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        userId="123456";
        //request.setAttribute("userId",123456);
        UserInfo userInfo = new UserInfo();
        Long userId = Long.valueOf(request.getHeader("userId"));

        userInfo.setUserId(userId);
        THREAD_LOCAL.set(userInfo);

        return true;
    }
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
