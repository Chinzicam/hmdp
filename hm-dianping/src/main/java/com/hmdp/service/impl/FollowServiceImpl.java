package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.FOLLOW_SIGN_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private IFollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    /**
     * 关注与取关
     *
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_SIGN_KEY + userId;
        //判断是否关注
        if (isFollow) {
            //关注，则将信息保存到数据库
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean save = followService.save(follow);
            if (save) {
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            //取关，则将数据从数据库中移除
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId);
            boolean remove = followService.remove(wrapper);
            if (remove) {
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询是否关注
     *
     * @param followUserId
     * @return
     */
    @Override
    public Result isFollow(Long followUserId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowUserId, followUserId);
        int count = followService.count(wrapper);
        return Result.ok(count > 0);
    }

    /**
     * 共同关注
     * @param id
     * @return
     */
    @Override
    public Result followCommons(Long id) {
        // 获取当前用户id
        Long userId = UserHolder.getUser().getId();
        String key1 = FOLLOW_SIGN_KEY + id;
        String key2 = FOLLOW_SIGN_KEY + userId;
        // 对当前用户和博主用户的关注列表取交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集就返回个空集合
            return Result.ok(Collections.emptyList());
        }
        // 将结果转为list
        List<Long> ids = new ArrayList<>();
        for (String value : intersect) {
            ids.add(Long.valueOf(value));
        }
        // 之后根据ids去查询共同关注的用户，封装成UserDto再返回
        List<UserDTO> userDTOS = new ArrayList<>();
        List<User> userList = userService.listByIds(ids);
        for (User user : userList) {
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            userDTOS.add(userDTO);
        }
        return Result.ok(userDTOS);
    }

}
