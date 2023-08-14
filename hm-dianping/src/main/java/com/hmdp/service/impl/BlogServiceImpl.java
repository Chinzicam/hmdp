package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询首页热门笔记，返回封面信息
     *
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryBlogById(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 根据id查询笔记信息，返回笔记信息和个人信息
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogById(Integer id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        queryBlogById(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }


    /**
     * 根据id查询昵称头像
     * @param blog
     */
    private void queryBlogById(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 笔记点赞功能
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        //获取当前用户信息
        Long userId = UserHolder.getUser().getId();
        //如果当前用户未点赞，则点赞数 +1，同时将用户加入set集合
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //为null，则表示集合中没有该用户,点赞数加一
        if (score == null) {
            boolean isSuccess = update().setSql("liked=liked+1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        //存在用户，则点赞数减一
        else {
            boolean success = update().setSql("liked=liked-1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 点赞排行榜
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Integer id) {
        String key=BLOG_LIKED_KEY+id;
        //zrange key 0 4  查询zset中前5个元素
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        //如果是空的(可能没人点赞)，直接返回一个空集合
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>();
        for (String value : top5) {
            ids.add(Long.valueOf(value));
        }

        // 将 ids 使用 ',' 拼接
        StringBuilder idsStr = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            idsStr.append(ids.get(i));
            if (i != ids.size() - 1) {
                idsStr.append(",");
            }
        }

        // 查询用户信息，按照 ids 的顺序排序
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        queryWrapper.last("order by field(id," + idsStr + ")");
        List<UserDTO> userDTOS = new ArrayList<>();
        List<User> userList = userService.list(queryWrapper);
        for (User user : userList) {
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            userDTOS.add(userDTO);
        }

        return Result.ok(userDTOS);
    }

    /**
     * 判断当前笔记是否点赞
     * @param blog
     */
    private void isBlogLiked(Blog blog) {

        UserDTO userDTO = UserHolder.getUser();
        //当用户未登录时，就不判断了，直接return结束逻辑
        if (userDTO == null) {
            return;
        }
        //2. 判断当前用户是否点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userDTO.getId().toString());
        blog.setIsLike(score != null);
    }
}
