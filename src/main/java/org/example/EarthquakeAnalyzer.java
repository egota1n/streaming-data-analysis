package org.example;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EarthquakeAnalyzer {
    private static final String PROCESSED_DATA_TOPIC = "earthquakes-processed";
    private static final Logger logger = LoggerFactory.getLogger(EarthquakeAnalyzer.class);

    private final Consumer<String, String> consumer;
    private final ObjectMapper objectMapper;

    public EarthquakeAnalyzer() {
        // 1. Настройка Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "earthquake-analyzer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(PROCESSED_DATA_TOPIC));

        // 2. Правильная настройка ObjectMapper
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void analyze() {
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        EarthquakeEvent event = objectMapper.readValue(record.value(), EarthquakeEvent.class);
                        logger.info("Processed earthquake: {}", event);

                        // Здесь можно добавить логику анализа
                        processEvent(event);

                    } catch (Exception e) {
                        logger.error("Error processing record from topic {}: {}", PROCESSED_DATA_TOPIC, record.value(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in consumer loop", e);
        } finally {
            try {
                consumer.close();
                logger.info("Kafka consumer closed successfully");
            } catch (Exception e) {
                logger.error("Error closing consumer", e);
            }
        }
    }

    private void processEvent(EarthquakeEvent event) {
        // Реализуйте здесь вашу логику анализа
        // Например:
        logger.debug("Processing event at {} (mag {}) in {}",
                event.getTimestamp(),
                event.getMagnitude(),
                event.getLocation());
    }
}