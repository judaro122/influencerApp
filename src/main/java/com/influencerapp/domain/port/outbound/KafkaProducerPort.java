package com.influencerapp.domain.port.outbound;
/**
 * Outbound port defining the contract for Kafka event publishing.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface KafkaProducerPort {

    void send(String topic, Object payload);
}
