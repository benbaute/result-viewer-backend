package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Date;
import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from intersection_ride_region_metrics")
public class IntersectionRideRegionMetrics extends TimeBaseClass implements PropertiesMappable {

	@Id
	private Long id;

	@Column(name = "region_id")
	private Long regionId;

	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime;

	@Column(name = "ride_id")
	private Long rideId;

	@Column(name = "name")
	private String name;

	@Column(name = "admin_level")
	private int adminLevel;

	@Column(name = "number_of_edges")
	private int numberOfEdges;

	@Column(name = "edge_length_km")
	private double edgeLength;

	@Column(name = "edge_duration")
	private double edgeDuration;

	@Column(name = "edge_waiting_time")
	private double edgeWaitingTime;

	@Column(name = "edge_median_waiting_time")
	private double edgeMedianWaitingTime;

	@Column(name = "number_of_nodes")
	private int numberOfNodes;

	@Column(name = "node_length_km")
	private double nodeLength;

	@Column(name = "node_duration")
	private double nodeDuration;

	@Column(name = "node_waiting_time")
	private double nodeWaitingTime;

	@Column(name = "node_median_waiting_time")
	private double nodeMedianWaitingTime;

	@Column(name = "length_km")
	private double length;

	@Column(name = "duration")
	private double duration;

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = this.getBaseProperties();
		properties.put("rideId", rideId);
		properties.put("regionId", regionId);
		properties.put("startTime", startTime);
		properties.put("name", name);
		properties.put("adminLevel", adminLevel);
		properties.put("numberOfEdges", numberOfEdges);
		properties.put("edgeLength", edgeLength);
		properties.put("edgeDuration", edgeDuration);
		properties.put("edgeWaitingTime", edgeWaitingTime);
		properties.put("edgeMedianWaitingTime", edgeMedianWaitingTime);
		properties.put("numberOfNodes", numberOfNodes);
		properties.put("nodeLength", nodeLength);
		properties.put("nodeDuration", nodeDuration);
		properties.put("nodeWaitingTime", nodeWaitingTime);
		properties.put("nodeMedianWaitingTime", nodeMedianWaitingTime);
		properties.put("length", length);
		properties.put("duration", duration);

		properties.put("nodesPerKm", numberOfNodes / length);
		properties.put("nodeWaitingRate", 100 * nodeWaitingTime / duration);
		properties.put("nodeWaitingSPerKm", nodeWaitingTime / length);
		properties.put("edgeWaitingSPerKm", edgeWaitingTime / length);

		return properties;
	}

}
