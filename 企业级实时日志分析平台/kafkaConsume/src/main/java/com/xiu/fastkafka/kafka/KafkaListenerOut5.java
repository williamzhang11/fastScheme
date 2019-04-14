package com.xiu.fastkafka.kafka;

import java.util.Date;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerOut5 {

	@KafkaListener(topics = {"test"},groupId="test")
    public void listen1(String msg) {
        System.err.println(new Date()+"kafka5的value: " + msg);
    }
}
