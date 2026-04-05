package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.locationtech.jts.geom.LineString;

import java.util.Map;

@Getter
@MappedSuperclass
public abstract class IntersectionBaseMetrics extends TimeBaseClass {

	@Id
	private Long id;

	@Column(name = "geom", columnDefinition = "geometry(LineString,4326)")
	private LineString geom;

	// @Column(name = "example_id")
	// private int exampleId; // TODO: add back if mqt fixed

	@Column(name = "number_of_rides")
	private int numberOfRides;

	@Column(name = "avg_length")
	private double avgLength;

	@Column(name = "avg_duration")
	private double avgDuration;

	@Column(name = "avg_speed")
	private double avgSpeed;

	@Column(name = "max_waiting_time")
	private double maxWaitingTime;

	@Column(name = "avg_waiting")
	private double avgWaitingTime;

	@Column(name = "stop_rate")
	private double stopRate;

	@Column(name = "avg_waiting_when_stopped")
	private double avgWaitingTimeWhenStopped;

	public Map<String, Object> getBaseProperties() {
		Map<String, Object> properties = super.getBaseProperties();
		// properties.put("id", exampleId);
		properties.put("numberOfRides", numberOfRides);
		properties.put("avgLength", avgLength);
		properties.put("avgDuration", avgDuration);
		properties.put("avgSpeed", avgSpeed);
		properties.put("maxWaitingTime", maxWaitingTime);
		properties.put("avgWaitingTime", avgWaitingTime);
		properties.put("stopRate", stopRate);
		properties.put("avgWaitingTimeWhenStopped", avgWaitingTimeWhenStopped);

		return properties;
	}

}
