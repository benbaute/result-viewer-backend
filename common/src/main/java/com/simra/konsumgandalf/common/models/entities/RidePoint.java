package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.util.Date;

/**
 * Contains metadata for a single ride.
 */
@Entity
public class RidePoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id", nullable = false)
	@JsonIgnore
	private Ride ride;

	@Column(columnDefinition = "geometry(Point,4326)", nullable = false)
	private Point geom;

	// --- Constructor ---
	public RidePoint() {
	}

	// --- Getter and Setter ---
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Ride getRide() {
		return ride;
	}

	public void setRide(Ride ride) {
		this.ride = ride;
	}

	public Point getGeom() {
		return geom;
	}

	public void setGeom(Point geom) {
		this.geom = geom;
	}

}
