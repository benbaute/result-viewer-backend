package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.Date;

/**
 * Contains metadata for a single ride.
 */
@Getter
@Setter
@Entity
public class MatchedPoint {

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

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

	// --- Data, not saved ---
	@Transient
	private String type;

	@Transient
	private int edgeIndex;

	@Transient
	private double distanceAlongEdge;

	@Transient
	private double distanceFromTracePoint;

	// --- Constructor ---
	public MatchedPoint() {
	}

    public boolean getInIntersection() {
        return this.inIntersection;
    }
}
