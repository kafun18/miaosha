package com.imooc.miaosha.controller;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.access.AccessLimit;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.redis.MiaoshaKey;

import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController{

	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	MiaoshaService miaoshaService;
	
	@RequestMapping(value="/do_miaosha",method=RequestMethod.POST)
	@ResponseBody
	public Result<OrderInfo> list(Model model,MiaoshaUser user,
			@RequestParam("goodsId")long goodsId){
		model.addAttribute("user", user);
		if(user==null){
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		//判断库存
		GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
		int stock=goods.getStockCount();
		if(stock<=0){
			//model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
			//return "miaosha_fail";
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		//判断是否已经秒杀到了
		MiaoshaOrder order=orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);	
		if(order!=null){
			//model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
			return Result.error(CodeMsg.REPEATE_MIAOSHA);
		}
		//减库存 下订单 写入秒杀订单
		OrderInfo orderInfo=miaoshaService.miaosha(user, goods);
		//model.addAttribute("orderIndo", orderInfo);
		//model.addAttribute("goods", goods);
		return Result.success(orderInfo);
		
	}
	
	@RequestMapping(value="/do_miaosha1",produces="text/html")
	public String list1(Model model,MiaoshaUser user,
			@RequestParam("goodsId")long goodsId){
		model.addAttribute("user", user);
		if(user==null){
			return "login";
		}
		//判断库存
		GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
		int stock=goods.getStockCount();
		if(stock<=0){
			model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
			return "miaosha_fail";
			//return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		//判断是否已经秒杀到了
		MiaoshaOrder order=orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);	
		if(order!=null){
			model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
			return "miaosha_fail";
		}
		//减库存 下订单 写入秒杀订单
		OrderInfo orderInfo=miaoshaService.miaosha(user, goods);
		model.addAttribute("orderIndo", orderInfo);
		model.addAttribute("goods", goods);
		return "order_detail";
		
	}
	
	
	
}
