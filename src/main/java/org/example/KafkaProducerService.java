package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaProducerService {
    private static final String RAW_DATA_TOPIC = "earthquakes-raw";
    private static final String PROCESSED_DATA_TOPIC = "earthquakes-processed";
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaProducerService() {
        this.producer = createKafkaProducer();
        this.objectMapper = createObjectMapper();
    }

    private Producer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Для надёжности
        props.put(ProducerConfig.RETRIES_CONFIG, 3); // Ретрии при ошибках

        return new KafkaProducer<>(props);
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void processEarthquakeData(String message) {
        try {
            // 1. Отправляем сырые данные
            sendToKafka(RAW_DATA_TOPIC, message, null);

            // 2. Парсим и валидируем сообщение
            JsonNode rootNode = objectMapper.readTree(message);
            if (!isValidEarthquakeMessage(rootNode)) {
                logger.warn("Invalid message structure: {}", message);
                return;
            }

            // 3. Создаём и отправляем обработанное событие
            EarthquakeEvent event = createEventFromProperties(rootNode.get("data").get("properties"));
            String processedData = objectMapper.writeValueAsString(event);
            sendToKafka(PROCESSED_DATA_TOPIC, processedData, event);

        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
        }
    }

    private void sendToKafka(String topic, String message, EarthquakeEvent event) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send message to {}: {}", topic, exception.getMessage());
                } else {
                    logger.debug("Sent to {}: {}", topic, event != null ? event : "raw data");
                }
            });
        } catch (Exception e) {
            logger.error("Kafka send error for topic {}: {}", topic, e.getMessage());
        }
    }

    private boolean isValidEarthquakeMessage(JsonNode rootNode) {
        try {
            JsonNode properties = rootNode.path("data").path("properties");
            return properties.has("time")
                    && properties.has("lat")
                    && properties.has("lon")
                    && properties.has("mag");
        } catch (Exception e) {
            return false;
        }
    }

    private EarthquakeEvent createEventFromProperties(JsonNode properties) throws IOException {
        return new EarthquakeEvent(
                Instant.parse(properties.get("time").asText()),
                properties.get("lat").asDouble(),
                properties.get("lon").asDouble(),
                properties.get("mag").asDouble(),
                properties.path("flynn_region").asText(properties.path("auth").asText("Unknown"))
        );
    }

    public void close() {
        try {
            producer.flush();
            producer.close();
            logger.info("Kafka producer closed successfully");
        } catch (Exception e) {
            logger.error("Error closing Kafka producer", e);
        }
    }
}