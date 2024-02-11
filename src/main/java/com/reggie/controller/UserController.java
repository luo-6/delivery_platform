package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.Result;
import com.reggie.pojo.User;
import com.reggie.service.UserService;
import com.reggie.utils.SMSUtils;
import com.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public Result<String> sentMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();
        if (!phone.isEmpty()){
            //随机生成验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code:{}",code);
            //调用阿里云短信api进行短信发送
            SMSUtils.sendMessage("瑞吉外卖","",phone,code);
            //需要将生成的验证码保存到session中
//            session.setAttribute(phone,code);

            //将生成的验证码转到redis并且设置有效期5分钟
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            Result.success("手机验证码短信发送成功");
        }
        return  Result.error("手机验证码短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody Map map,HttpSession session){
        //获取手机号
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //从Session中获取保存的验证码
//        String codeInSession = session.getAttribute(phone).toString();
        //从redis中获取缓存验证码
        Object codeInSession = redisTemplate.opsForValue().get(phone);
        //进行验证码的比对
        if (codeInSession != null && codeInSession.equals(code)){
            //如果比对成功就登陆成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(queryWrapper);
            if (user == null){
                //判断当前手机号是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            log.info("信息验证成功");
            //如果登陆成功,删除验证码
            redisTemplate.delete(phone);
            return Result.success(user);
        }

        return Result.error("信息验证失败");
    }

    /**
     * 实现登出操作
     * @return
     */
    @PostMapping("/loginout")
    public Result<String> loginout(){
        BaseContext.setCurrentId(null);
        return Result.success("登出成功");
    }
}
