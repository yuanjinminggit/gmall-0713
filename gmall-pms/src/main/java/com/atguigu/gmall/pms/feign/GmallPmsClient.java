package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.spi.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("sms-service")
public interface GmallPmsClient extends GmallSmsApi {

}
