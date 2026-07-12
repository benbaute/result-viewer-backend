package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.Identifiable;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Map;

@Getter
@MappedSuperclass
public abstract class IntersectionRegionBaseMetrics extends TimeBaseClass implements Identifiable {

	@Id
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id")
	private Region region;

	@Column(name = "number_of_edges")
	private int numberOfEdges;

	@Column(name = "edge_length_km")
	private double edgeLength;

	@Column(name = "edge_duration")
	private double edgeDuration;

	@Column(name = "edge_waiting_time")
	private double edgeWaitingTime;

	@Column(name = "edge_stop_count")
	private double edgeStopCount;

	@Column(name = "edge_stop_time")
	private double edgeStopTime;

	@Column(name = "number_of_nodes")
	private int numberOfNodes;

	@Column(name = "node_length_km")
	private double nodeLength;

	@Column(name = "node_duration")
	private double nodeDuration;

	@Column(name = "node_waiting_time")
	private double nodeWaitingTime;

	@Column(name = "node_stop_count")
	private double nodeStopCount;

	@Column(name = "node_stop_time")
	private double nodeStopTime;

	public Map<String, Object> getRegionBaseProperties() {
		Map<String, Object> properties = this.getBaseProperties();
		double length = edgeLength + nodeLength;
		double duration = edgeDuration + nodeDuration;
		properties.put("length", length);
		properties.put("duration", duration);

		properties.put("numberOfEdges", numberOfEdges);
		properties.put("edgeLength", edgeLength);
		properties.put("edgeDuration", edgeDuration);
		properties.put("edgeWaitingTime", edgeWaitingTime);
		properties.put("edgeAvgWaitingTime", edgeWaitingTime / numberOfEdges);
		properties.put("edgeAvgWaitingTimeWhenStopped", edgeStopTime / numberOfEdges);
		properties.put("edgeStopRate", 100 * edgeStopCount / numberOfEdges);
		properties.put("edgeWaitingSPerKm", edgeWaitingTime / length);
		properties.put("edgeWaitingRate", 100 * edgeWaitingTime / duration);

		properties.put("numberOfNodes", numberOfNodes);
		properties.put("nodeLength", nodeLength);
		properties.put("nodeDuration", nodeDuration);
		properties.put("nodeWaitingTime", nodeWaitingTime);
		properties.put("nodeAvgWaitingTime", nodeWaitingTime / numberOfNodes);
		properties.put("nodeAvgWaitingTimeWhenStopped", nodeStopTime / numberOfNodes);
		properties.put("nodeStopRate", 100 * nodeStopCount / numberOfNodes);
		properties.put("nodeWaitingSPerKm", nodeWaitingTime / length);
		properties.put("nodeWaitingRate", 100 * nodeWaitingTime / duration);

		properties.put("nodesPerKm", numberOfNodes / length);

		return properties;
	}

}