package com.simra.konsumgandalf.common.models.entities;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Point;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

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

	@Column(columnDefinition = "geometry(Point,4326)", nullable = false)
	private Point geom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "osm_id")
    private PlanetOsmLine osmLine;

    @OneToOne
    @JoinColumn(name = "ride_point_id")
    private RidePoint ridePoint;

    @ManyToMany(mappedBy = "matchedPoints", fetch = FetchType.LAZY)
    private List<IntersectionBase> intersections;

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
        properties.put("ridePointId", ridePoint.getId());
        if (intersections != null && intersections.size() == 1) {
            properties.put("intersectionId",  intersections.getFirst().getId());
        }
        properties.put("rideId", ride.getId());
        properties.put("timestamp", timestamp);
        properties.put("accuracy", accuracy);
        properties.put("osmId", osmLine != null ? osmLine.getId() : null);
        properties.put("inIntersection", inIntersection);
        properties.put("distanceFromTracePoint", distanceFromTracePoint);
        properties.put("stops", stops);
        return properties;
    }
}
