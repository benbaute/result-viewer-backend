package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.*;
import lombok.Getter;
import org.locationtech.jts.geom.LineString;

import java.util.HashMap;
import java.util.Map;

@Getter
@MappedSuperclass
public abstract class IntersectionBaseClassMetrics {
    @Id
    @Column(name = "traffic_time", length = 21)
    @Enumerated(EnumType.STRING)
    private TrafficTimes trafficTime;

    @Id
    @Column(name = "week_day", length = 12)
    @Enumerated(EnumType.STRING)
    private WeekDays weekDay;

    @Id
    @Column(name = "year")
    private Integer year;

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
        Map<String, Object> properties = new HashMap<>();
        properties.put("trafficTime", trafficTime);
        properties.put("weekDay", weekDay);
        properties.put("year", year);
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
