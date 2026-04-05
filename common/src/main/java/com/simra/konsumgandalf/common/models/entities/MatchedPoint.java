package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.locationtech.jts.geom.Point;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matched (with Valhalla) GPS point for a ride.
 */
@Getter
@Setter
@Entity
public class MatchedPoint implements FeatureMappable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long valhallaEdgeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id", nullable = false)
	private Ride ride;

	@Column(name = "ride_id", insertable = false, updatable = false)
	private Long rideId;

	@Column(columnDefinition = "geometry(Point,4326)", nullable = false)
	private Point geom;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "osm_id")
	private PlanetOsmLine osmLine;

	@Column(name = "osm_id", insertable = false, updatable = false)
	private Long osmLineId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_point_id")
	private RidePoint ridePoint;

	@Column(name = "ride_point_id", insertable = false, updatable = false)
	private Long ridePointId;

	@ManyToMany
	@JoinTable(name = "intersection__matched_points", joinColumns = @JoinColumn(name = "matched_point_id"),
			inverseJoinColumns = @JoinColumn(name = "intersection_id"))
	private List<IntersectionBase> intersections;

	@Formula("(SELECT CASE WHEN count(j.intersection_id) = 1 THEN min(j.intersection_id) ELSE null END "
			+ "FROM intersection__matched_points j " + "WHERE j.matched_point_id = id)")
	private Long singleIntersectionId;

	private boolean inIntersection;

	private Double distanceFromTracePoint;

	private int stops;

	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp;

	private double accuracy;

	// --- Data, not saved ---
	@Transient
	private String matchingResult;

	@Transient
	private Integer edgeIndex;

	@Transient
	private List<TrafficSignalCluster> trafficSignalClusters;

	@Transient
	private TrafficSignalCluster inIntersectionCluster;

	@Transient
	private PlanetOsmLine prevOsmLine;

	@Transient
	private PlanetOsmLine nextOsmLine;

	@Transient
	private Long prevValhallaEdgeId;

	@Transient
	private Long nextValhallaEdgeId;

	@Transient
	private Point rawGPSLocation;

	@Transient
	private Point prevRawGPSLocation;

	// --- Constructor ---
	public MatchedPoint() {
	}

	public boolean getInIntersection() {
		return this.inIntersection;
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("id", id);
		properties.put("ridePointId", ridePointId);
		properties.put("intersectionId", singleIntersectionId);
		properties.put("rideId", rideId);
		properties.put("timestamp", timestamp);
		properties.put("accuracy", accuracy);
		properties.put("osmId", osmLineId);
		properties.put("inIntersection", inIntersection);
		properties.put("distanceFromTracePoint", distanceFromTracePoint);
		properties.put("stops", stops);
		return properties;
	}

}
