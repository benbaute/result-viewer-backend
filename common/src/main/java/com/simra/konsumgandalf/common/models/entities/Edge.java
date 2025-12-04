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
public class Edge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id", nullable = false)
	private Ride ride;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "osm_id", nullable = false)
	private PlanetOsmLine line;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    private double speed;

    private Boolean direction;

	private double length;

	// --- Constructor ---
	public Edge() {
	}
}
