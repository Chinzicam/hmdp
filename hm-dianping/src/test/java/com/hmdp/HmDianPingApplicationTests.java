package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private IShopService shopService;
    @Test
    void testSaveShop() {
        shopService.saveShopRedis(1L, 10L);
    }

    @Test
    void testSaveShop1() {
        System.out.println("aaa");
    }

}
