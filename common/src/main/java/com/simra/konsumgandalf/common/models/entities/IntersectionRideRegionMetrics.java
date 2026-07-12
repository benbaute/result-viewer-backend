package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Date;
import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "intersection_ride_region_metrics")
public class IntersectionRideRegionMetrics extends IntersectionRegionBaseMetrics implements PropertiesMappable {

	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime;

	@Column(name = "ride_id")
	private Long rideId;

	@Column(name = "median_speed")
	private double medianRideSpeed;

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = this.getRegionBaseProperties();
		properties.put("rideId", rideId);
		properties.put("regionId", this.getRegion().getId());
		properties.put("startTime", startTime);
		properties.put("medianRideSpeed", medianRideSpeed);

		return properties;
	}

}
