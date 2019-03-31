package com.xiu.fastkafka.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerOut2 {

	@KafkaListener(topics = {"topic_zzh_test"},groupId="group2")
    public void listen1(String msg) {
        System.err.println("kafka2的value: " + msg);
    }
}
