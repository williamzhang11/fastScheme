package com.xiu.register.controller;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiu.register.server.ConsumerServer;

@RestController()
public class ConsumerController {

	@Autowired
	ConsumerServer consumerServer;
	
	@RequestMapping("/consumer")
	public String helloConsumer() throws InterruptedException, ExecutionException {
		
		return consumerServer.helloService();
	}
	
	
}
