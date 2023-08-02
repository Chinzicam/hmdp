package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IShopTypeService typeService;
    @Override
    public Result queryTypeList() {
        List<ShopType> typeList = (List<ShopType>) redisTemplate.opsForValue().get("typeList");

        if (typeList != null) {
            log.info("typelist不为空,不写入数据");
            return Result.ok(typeList);
        }
        typeList = typeService.query().orderByAsc("sort").list();
        // 将查询结果写入Redis缓存
        log.info("已将查询结果写入Redis缓存");
        redisTemplate.opsForValue().set("typeList", typeList,CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(typeList);
    }
}
