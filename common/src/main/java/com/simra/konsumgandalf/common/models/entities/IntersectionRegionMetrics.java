package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.locationtech.jts.geom.Polygon;

import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from intersection_region_metrics")
public class IntersectionRegionMetrics extends TimeBaseClass implements FeatureMappable {

	@Id
	private Long id;

	@Column(name = "region_id")
	private Long regionId;

	@Column(name = "geom", columnDefinition = "geometry(Polygon,4326)")
	private Polygon geom;

	@Column(name = "name")
	private String name;

	@Column(name = "admin_level")
	private int adminLevel;

	@Column(name = "number_of_rides")
	private int numberOfRides;

	@Column(name = "length_km")
	private double length;

	@Column(name = "duration")
	private double duration;

	@Column(name = "number_of_edges")
	private int numberOfEdges;

	@Column(name = "edge_length_km")
	private double edgeLength;

	@Column(name = "edge_duration")
	private double edgeDuration;

	@Column(name = "edge_waiting_time")
	private double edgeWaitingTime;

	@Column(name = "edge_avg_waiting_time")
	private double edgeAvgWaitingTime;

	@Column(name = "edge_avg_waiting_when_stopped")
	private double edgeAvgWaitingTimeWhenStopped;

	@Column(name = "edge_stop_rate")
	private double edgeStopRate;

	@Column(name = "edge_waiting_s_per_km")
	private double edgeWaitingSPerKm;

	@Column(name = "edge_waiting_rate")
	private double edgeWaitingRate;

	@Column(name = "number_of_nodes")
	private int numberOfNodes;

	@Column(name = "node_length_km")
	private double nodeLength;

	@Column(name = "node_duration")
	private double nodeDuration;

	@Column(name = "node_waiting_time")
	private double nodeWaitingTime;

	@Column(name = "node_avg_waiting_time")
	private double nodeAvgWaitingTime;

	@Column(name = "node_avg_waiting_when_stopped")
	private double nodeAvgWaitingTimeWhenStopped;

	@Column(name = "node_stop_rate")
	private double nodeStopRate;

	@Column(name = "node_waiting_s_per_km")
	private double nodeWaitingSPerKm;

	@Column(name = "node_waiting_rate")
	private double nodeWaitingRate;

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = this.getBaseProperties();
		properties.put("id", regionId);
		properties.put("name", name);
		properties.put("adminLevel", adminLevel);
		properties.put("numberOfRides", numberOfRides);
		properties.put("length", length);
		properties.put("duration", duration);

		properties.put("numberOfEdges", numberOfEdges);
		properties.put("edgeLength", edgeLength);
		properties.put("edgeDuration", edgeDuration);
		properties.put("edgeWaitingTime", edgeWaitingTime);
		properties.put("edgeAvgWaitingTime", edgeAvgWaitingTime);
		properties.put("edgeAvgWaitingTimeWhenStopped", edgeAvgWaitingTimeWhenStopped);
		properties.put("edgeStopRate", edgeStopRate);
		properties.put("edgeWaitingSPerKm", edgeWaitingSPerKm);
		properties.put("edgeWaitingRate", edgeWaitingRate);

		properties.put("numberOfNodes", numberOfNodes);
		properties.put("nodeLength", nodeLength);
		properties.put("nodeDuration", nodeDuration);
		properties.put("nodeWaitingTime", nodeWaitingTime);
		properties.put("nodeAvgWaitingTime", nodeAvgWaitingTime);
		properties.put("nodeAvgWaitingTimeWhenStopped", nodeAvgWaitingTimeWhenStopped);
		properties.put("nodeStopRate", nodeStopRate);
		properties.put("nodeWaitingSPerKm", nodeWaitingSPerKm);
		properties.put("nodeWaitingRate", nodeWaitingRate);

		properties.put("nodesPerKm", numberOfNodes / length);

		return properties;
	}

}
