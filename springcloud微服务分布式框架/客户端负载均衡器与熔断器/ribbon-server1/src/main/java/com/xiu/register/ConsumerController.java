package com.xiu.register;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController()
public class ConsumerController {

	@Autowired
	private RestTemplate restTemplate;
	
	@RequestMapping("/consumer")
	public String helloConsumer() {
		System.out.println("test");
		return restTemplate.getForEntity("http://helloservice/hello", String.class).getBody();
	}
}
