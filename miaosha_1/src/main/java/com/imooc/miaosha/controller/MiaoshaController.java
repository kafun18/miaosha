package com.imooc.miaosha.controller;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.*;

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
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.MiaoshaKey;

import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.imooc.miaosha.vo.GoodsVo;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean{

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
	
	@Autowired
	MQSender sender;
	
	private HashMap<Long, Boolean> localOverMap =  new HashMap<Long, Boolean>();
	
	/**
	 * 系统初始化
	 * */
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
		for(GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getStockCount());
			localOverMap.put(goods.getId(), false);
		}
		
	}
	
	@RequestMapping(value="/{path}/do_miaosha",method=RequestMethod.POST)
	@ResponseBody
	public Result<Integer> list(Model model,MiaoshaUser user,
			@RequestParam("goodsId")long goodsId,
			@PathVariable("path")String path){
		model.addAttribute("user", user);
		if(user==null){
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		//验证
		boolean check = miaoshaService.checkPath(user, goodsId, path);
        if(!check){
        	return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
		//内存标记，减少redis访问
		boolean over = localOverMap.get(goodsId);
		if(over){
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		    	
    	//预减库存
    	long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, ""+goodsId);//10
    	if(stock < 0) {
    		 localOverMap.put(goodsId, true);
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
    	}
    	//判断是否已经秒杀到了
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}
    	//入队
    	MiaoshaMessage mm = new MiaoshaMessage();
    	mm.setUser(user);
    	mm.setGoodsId(goodsId);
    	sender.sendMiaoshaMessage(mm);
    	return Result.success(0);//排队中
		
		//判断库存
		//GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
		//int stock=goods.getStockCount();
		//if(stock<=0){
			/*model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
			return "miaosha_fail";*/
			//return Result.error(CodeMsg.MIAO_SHA_OVER);
		//}
		//判断是否已经秒杀到了
		//MiaoshaOrder order=orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);	
		//if(order!=null){
			/*model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());*/
			//return Result.error(CodeMsg.REPEATE_MIAOSHA);
		//}
		//减库存 下订单 写入秒杀订单
		//OrderInfo orderInfo=miaoshaService.miaosha(user, goods);
		/*model.addAttribute("orderIndo", orderInfo);
		model.addAttribute("goods", goods);*/
		//return Result.success(orderInfo);
		
	}
	
	/**
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     * */
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,MiaoshaUser user,
    		@RequestParam("goodsId")long goodsId) {
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
    	return Result.success(result);
    }
    
    @RequestMapping(value="/path",method=RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaPath(Model model,MiaoshaUser user,
			@RequestParam("goodsId")long goodsId){
		model.addAttribute("user", user);
		if(user==null){
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		String path = miaoshaService.createMiaoshaPath(user, goodsId);
		//String str = MD5Util.md5(UUIDUtil.uuid()+"123456");
		//redisService.set(MiaoshaKey.getMiaoshaPath, ""+user.getId()+"_"+goodsId, str);
		return Result.success(path);
        
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
