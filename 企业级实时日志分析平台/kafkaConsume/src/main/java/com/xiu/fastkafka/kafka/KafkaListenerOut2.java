package com.xiu.fastkafka.kafka;

import java.util.Date;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerOut2 {

	@KafkaListener(topics = {"test"},groupId="test")
    public void listen1(String msg) {
        System.err.println(new Date()+"kafka2的value: " + msg);
    }
}
