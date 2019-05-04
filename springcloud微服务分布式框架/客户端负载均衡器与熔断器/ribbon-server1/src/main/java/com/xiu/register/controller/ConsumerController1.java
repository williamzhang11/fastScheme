package com.xiu.register.controller;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.xiu.register.server.HelloServiceCommand;

@RestController
public class ConsumerController1 {

    @Autowired
    private  RestTemplate restTemplate;
    
    @RequestMapping("/consumer1")
    public String helloConsumer() throws InterruptedException, ExecutionException{

        HelloServiceCommand command = new HelloServiceCommand("hello",restTemplate);
        //String result = command.execute();
        Future<String> queue=command.queue();//Future,异步io,需要的时候通过future.get去取
        return queue.get();
    }
}
