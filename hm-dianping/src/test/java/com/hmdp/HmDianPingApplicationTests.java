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

    //给逻辑过期的数据提前做缓存
    @Test
    void testSaveShop() {
        shopService.saveShopRedis(2L, 2L);
    }

    @Test
    void testSaveShop1() {
        System.out.println("aaa");
    }

}
