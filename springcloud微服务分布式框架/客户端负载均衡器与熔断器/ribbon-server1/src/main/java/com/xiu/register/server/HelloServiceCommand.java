package com.xiu.register.server;

import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class HelloServiceCommand extends HystrixCommand<String>{
	private RestTemplate restTemplate;

    public HelloServiceCommand(String commandGroupKey,RestTemplate restTemplate) {
        super(HystrixCommandGroupKey.Factory.asKey(commandGroupKey));
        this.restTemplate = restTemplate;
    }

	@Override
	protected String run() throws Exception {
		return restTemplate.getForEntity("http://helloservice/hello", String.class).getBody();
	}
	
	@Override
    protected String getFallback() {
        return "error";
    }

}
