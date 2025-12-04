package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

import java.util.Date;


@Getter
@Setter
@Entity
public class IntersectionDelay {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id", nullable = false)
	private Ride ride;

	@Column(columnDefinition = "geometry(LineString,4326)", nullable = false)
	private LineString geom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_osm_id")
    private PlanetOsmLine startLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_osm_id")
    private PlanetOsmLine endLine;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    private double speed;

    private double duration;

    private double length;

	// --- Constructor ---
	public IntersectionDelay() {
	}
}
