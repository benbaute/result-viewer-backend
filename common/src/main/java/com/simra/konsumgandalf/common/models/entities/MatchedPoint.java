package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id", nullable = false)
	private Ride ride;

	@Column(columnDefinition = "geometry(Point,4326)", nullable = false)
	private Point geom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "osm_id")
    private PlanetOsmLine line;

    private boolean inIntersection;

    private int edgeId;

    private int pointInEdgeId;

    private Double distanceFromTracePoint;

    private int stops;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

	// --- Data, not saved ---
    @Transient
    private Long osmId;

	@Transient
	private String type;

	@Transient
	private Integer edgeIndex;

    @Transient
    Coordinate coordinate;

    @Transient
    private List<TrafficSignalCluster> trafficSignalClusters;

    @Transient
    private TrafficSignalCluster inIntersectionCluster;

    @Transient
    private PlanetOsmLine prevLine;

    @Transient
    private PlanetOsmLine nextLine;


	// --- Constructor ---
	public MatchedPoint() {
	}

    public boolean getInIntersection() {
        return this.inIntersection;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("timestamp", this.getTimestamp());
        properties.put("edge_id", this.getEdgeId());
        properties.put("point_in_edge_id", this.getPointInEdgeId());
        properties.put("way_id", this.getLine() != null ? this.getLine().getId() : "null");
        properties.put("inIntersection", this.getInIntersection());
        properties.put("ride_id", this.ride.getId());
        properties.put("distance_from_trace_point", this.getDistanceFromTracePoint());
        properties.put("stops", this.getStops());
        return properties;
    }
}
