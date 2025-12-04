package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.simra.konsumgandalf.common.models.classes.RideLoc;
import com.simra.konsumgandalf.common.models.classes.RideLocation;
import com.simra.konsumgandalf.common.models.enums.BikeType;
import com.simra.konsumgandalf.common.models.enums.PhoneLocation;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.maps.TrafficTimesMapper;
import jakarta.persistence.*;
import org.geolatte.geom.Geometry;
import java.util.*;

import static com.simra.konsumgandalf.common.constants.AppDates.FALLBACK_DATE_MILLIS;

/**
 * Contains metadata for a single ride.
 */
@Entity
public class Ride extends TimeBaseClass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// --- Metadata ---
	@Temporal(TemporalType.TIMESTAMP)
	private Date rideStart;

	@Temporal(TemporalType.TIMESTAMP)
	private Date rideEnd;

	@Column(unique = true)
	private String path;

	// --- Links to other tables ---
	@OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RidePoint> points = new ArrayList<>();

	@OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RidePoint> matchedPoints = new ArrayList<>();

	// --- Data, not saved ---

	@Transient
	private List<RideLoc> rideLocations = new ArrayList<>();

	// --- Constructor ---
	public Ride(String path) {
		this.path = path;
	}

	public Ride() {
	}

	// --- Getter and Setter ---
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Date getRideStart() {
		return rideStart;
	}

	public void setRideStart(Date rideStart) {
		this.rideStart = rideStart;
	}

	public Date getRideEnd() {
		return rideEnd;
	}

	public void setRideEnd(Date rideEnd) {
		this.rideEnd = rideEnd;
	}

	public List<RidePoint> getPoints() {
		return points;
	}

	public void setPoints(List<RidePoint> points) {
		this.points = points;
	}

	public List<RideLoc> getRideLocations() {
		return rideLocations;
	}

	public void setRideLocations(List<RideLoc> rideLocation) {
		this.rideLocations = rideLocation;
	}

}
