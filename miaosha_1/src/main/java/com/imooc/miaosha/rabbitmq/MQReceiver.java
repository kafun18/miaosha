package com.imooc.miaosha.rabbitmq;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {

	private static Logger log=LoggerFactory.getLogger(MQConfig.class);
	//监听消息
	@RabbitListener(queues=MQConfig.QUEUE)
	public void receive(String message){
		log.info("receive message:"+message);
	}
}
