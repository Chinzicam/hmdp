package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 查询优惠券
//        LambdaQueryWrapper<SeckillVoucher> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(SeckillVoucher::getVoucherId,voucherId);
//        SeckillVoucher seckillVoucher = seckillVoucherService.getOne(wrapper);

        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (LocalDateTime.now().isBefore(voucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始！");
        }
        //判断秒杀是否结束
        if (LocalDateTime.now().isAfter(voucher.getEndTime())) {
            return Result.fail("秒杀已经结束！");
        }
        //判断是否还有库存
        if (voucher.getStock() < 1) {
            return Result.fail("判断秒杀是否开始,库存不足！");
        }
        //创建订单
        return createVoucherOrder(voucherId);
    }

    /**
     * 创建订单
     * @param voucherId
     * @return
     */
    private Result createVoucherOrder(Long voucherId) {
        //扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足！");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //设置订单id
        long orderId = redisIdWorker.nextId("order");
        //设置用户id
        Long id = UserHolder.getUser().getId();
        //设置代金券id
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(id);
        //将订单数据保存到表中
        save(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }
}
