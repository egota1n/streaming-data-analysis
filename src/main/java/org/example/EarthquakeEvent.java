package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Instant;

@JsonSerialize
public class EarthquakeEvent {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant timestamp;
    private final double latitude;
    private final double longitude;
    private final double magnitude;
    private final String location;

    @JsonCreator
    public EarthquakeEvent(
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("magnitude") double magnitude,
            @JsonProperty("location") String location) {
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.magnitude = magnitude;
        this.location = location;
    }

    // Геттеры остаются без изменений
    public Instant getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getMagnitude() { return magnitude; }
    public String getLocation() { return location; }

    @Override
    public String toString() {
        return String.format(
                "EarthquakeEvent{time=%s, latitude=%.3f, longitude=%.3f, magnitude=%.1f, location='%s'}",
                timestamp, latitude, longitude, magnitude, location
        );
    }
}