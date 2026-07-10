package com.example.paymentservice.kafka;

import com.example.paymentservice.dto.OrderEvent;
import com.example.paymentservice.entity.FailedEvent;
import com.example.paymentservice.repository.FailedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DltConsumer {
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-dlt", groupId = "payment-dlt-group")
    public void consumeDlt(OrderEvent event,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("DLQ Consumer received failed event from topic: {}. Event details: {}. Exception: {}",
                topic, event, exceptionMessage);

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            
            FailedEvent failedEvent = FailedEvent.builder()
                    .eventId(event.getEventId() != null ? event.getEventId() : "N/A")
                    .topicName(topic)
                    .payload(payloadJson)
                    .errorMessage(exceptionMessage != null ? exceptionMessage : "Unknown exception")
                    .failedAt(LocalDateTime.now())
                    .build();

            failedEventRepository.save(failedEvent);
            log.info("Successfully persisted failed event to database. Event ID: {}", failedEvent.getEventId());
        } catch (Exception e) {
            log.error("Failed to persist DLQ event to database", e);
        }
    }
}
