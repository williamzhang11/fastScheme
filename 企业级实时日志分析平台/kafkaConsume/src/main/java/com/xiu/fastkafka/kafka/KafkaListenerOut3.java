package com.xiu.fastkafka.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerOut3 {

	@KafkaListener(topics = {"topic_zzh_test"},groupId="group3")
    public void listen1(String msg) {
        System.err.println("kafka3的value: " + msg);
    }
}
