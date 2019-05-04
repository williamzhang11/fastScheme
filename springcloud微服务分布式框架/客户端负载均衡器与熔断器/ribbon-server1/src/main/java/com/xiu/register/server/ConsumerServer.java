package com.xiu.register.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.command.AsyncResult;

@Service
public class ConsumerServer {

	@Autowired
	private RestTemplate restTemplate;
	
	@HystrixCommand(fallbackMethod="consumerFackback")
	public String consumerServer() {
		 return restTemplate.getForEntity("http://helloservice/hello", String.class).getBody();
	}
	@HystrixCommand(fallbackMethod="consumerFackback")
	public String helloService() throws InterruptedException, ExecutionException {
		Future<String> future = new AsyncResult<String>() {
			@Override
			public String invoke() {
				return restTemplate.getForEntity("http://helloservice/hello", String.class).getBody();
            }
		};
        return future.get();
	}
	
	
	
	public String consumerFackback() {
		return "error";
	}
}
