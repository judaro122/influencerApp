package com.influencerapp.infrastructure.adapter.kafka;

import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProducerAdapter implements KafkaProducerPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(String topic, Object payload) {
        throw new UnsupportedOperationException("Not implemented in Phase 1");
    }
}
