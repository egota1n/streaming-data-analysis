package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.EarthquakeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class KafkaProducerServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerServiceTest.class);
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void testObjectMapperSerialization() throws JsonProcessingException {
        EarthquakeEvent event = new EarthquakeEvent(
                Instant.now(),
                42.8482,
                76.7747,
                4.1,
                "KYRGYZSTAN"
        );

        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("KYRGYZSTAN"));
        logger.info("Serialized event: {}", json);
    }

    @Test
    void testDeserialization() throws JsonProcessingException {
        String json = "{\"timestamp\":\"2025-05-10T15:35:21.925Z\",\"latitude\":42.8482," +
                "\"longitude\":76.7747,\"magnitude\":4.1,\"location\":\"KYRGYZSTAN\"}";

        EarthquakeEvent event = objectMapper.readValue(json, EarthquakeEvent.class);

        assertEquals(42.8482, event.getLatitude());
        assertEquals("KYRGYZSTAN", event.getLocation());
        assertEquals(Instant.parse("2025-05-10T15:35:21.925Z"), event.getTimestamp());
    }
}