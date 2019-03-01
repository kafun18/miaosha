package com.imooc.miaosha.redis;

import com.imooc.miaosha.redis.BasePrefix;

public class MiaoshaKey extends BasePrefix{

	private MiaoshaKey(String prefix) {
		super(prefix);
	}
	public static MiaoshaKey isGoodsOver = new MiaoshaKey( "go");
	public static MiaoshaKey getMiaoshaPath = new MiaoshaKey( "mp");
	public static MiaoshaKey getMiaoshaVerifyCode = new MiaoshaKey("vc");
}
