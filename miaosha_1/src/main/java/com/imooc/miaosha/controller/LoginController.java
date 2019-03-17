package com.imooc.miaosha.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.vo.LoginVo;

@Controller
@RequestMapping("/login")
public class LoginController {

	private static Logger log = LoggerFactory.getLogger(LoginController.class);
	
	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";
    }
    
    @RequestMapping("/do_login")
    @ResponseBody
    //@Valid用于验证注解是否符合要求，直接加在变量user之前，
    //在变量中添加验证信息的要求，当不符合要求时就会在方法中返回message的错误提示信息
    public Result<Boolean> doLogin(HttpServletResponse response,@Valid LoginVo loginVo) {
    	log.info(loginVo.toString());
    	//登录
    	Boolean token = userService.login(response, loginVo);
    	return Result.success(token);
    	//return null;
    }
}
