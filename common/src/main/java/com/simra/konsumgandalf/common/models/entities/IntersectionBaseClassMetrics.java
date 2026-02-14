package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.locationtech.jts.geom.LineString;

import java.util.Map;

@Getter
@MappedSuperclass
public abstract class IntersectionBaseClassMetrics extends TimeBaseClass {

    @Id
    private Long id;

    @Column(name="geom", columnDefinition = "geometry(LineString,4326)")
    private LineString geom;

    @Column(name = "example_id")
    private int exampleId;

    @Column(name = "number_of_rides")
    private int numberOfRides;

    @Column(name = "median_length")
    private double medianLength;

    @Column(name = "median_duration")
    private double medianDuration;

    @Column(name = "median_speed")
    private double medianSpeed;

    @Column(name = "max_waiting_time")
    private double maxWaitingTime;

    @Column(name = "median_waiting_time")
    private double medianWaitingTime;

    public Map<String, Object> getBaseProperties() {
        Map<String, Object> properties = super.getBaseProperties();
        properties.put("id", exampleId);
        properties.put("numberOfRides", numberOfRides);
        properties.put("medianLength", medianLength);
        properties.put("medianDuration", medianDuration);
        properties.put("medianSpeed", medianSpeed);
        properties.put("maxWaitingTime", maxWaitingTime);
        properties.put("medianWaitingTime", medianWaitingTime);
        return properties;
    }
}
