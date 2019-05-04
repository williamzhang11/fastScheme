package com.xiu.register.server;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "helloservice",fallback = FeignFallBack.class)
public interface FeignService {
	    @RequestMapping("/hello")
	    String hello();

	    @RequestMapping(value = "/hellol", method= RequestMethod.GET)
	    String hello(@RequestParam("name") String name) ;

	    @RequestMapping(value = "/hello2", method= RequestMethod.GET)
	    User hello(@RequestHeader("name") String name, @RequestHeader("age") Integer age);

	    @RequestMapping(value = "/hello3", method= RequestMethod.POST)
	    String hello(@RequestBody User user);
	}