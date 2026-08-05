package com.influencerapp.domain.port.outbound;

public interface KafkaProducerPort {

    void send(String topic, Object payload);
}
