package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.common.models.maps.TrafficTimesMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.*;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class IntersectionBase extends TimeBaseClass implements FeatureMappable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id", nullable = false)
	private Ride ride;

	@Column(columnDefinition = "geometry(LineString,4326)", nullable = false)
	private LineString geom;

	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date endTime;

	@Column
	private double speed; // km/h

	@Column
	private double medianSpeed; // km/h

	@Column
	private double duration; // s

	@Column
	private double length; // m

	@Column
	private Double waitingTime; // s

	@OneToOne
	@JoinColumn(name = "prev_intersection_id")
	private IntersectionBase prevIntersection;

	@OneToOne(mappedBy = "prevIntersection")
	private IntersectionBase nextIntersection;

	@Transient
	private Integer indexInRide;

	@ManyToMany
	@JoinTable(name = "intersection__matched_points", joinColumns = @JoinColumn(name = "intersection_id"),
			inverseJoinColumns = @JoinColumn(name = "matched_point_id"))
	private List<MatchedPoint> matchedPoints = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "intersection__region", joinColumns = @JoinColumn(name = "intersection_id"),
			inverseJoinColumns = @JoinColumn(name = "region_id"))
	private Set<Region> regions = new HashSet<>();

	// For calculating region
	@Transient
	private Point startPoint;

	public IntersectionBase() {
	}

	@PrePersist
	private void calculateTrafficTimesAndWeekDays() {
		Date date = this.getStartTime();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);

		super.setYear(calendar.get(Calendar.YEAR));
		super.setWeekDay(calendar.get(Calendar.DAY_OF_WEEK) - 1 <= 5 ? WeekDays.WEEK : WeekDays.WEEKEND);
		super.setTrafficTime(TrafficTimesMapper.getTrafficTime(date));
	}

	public void calculateAndSetWaitingTime(double rideSpeed) {
		this.waitingTime = duration - (length / rideSpeed);
	}

	public Map<String, Object> getBaseProperties() {
		Map<String, Object> properties = super.getBaseProperties();
		properties.put("nextIntersectionId", nextIntersection != null ? nextIntersection.getId() : null);
		properties.put("prevIntersectionId", prevIntersection != null ? prevIntersection.getId() : null);
		properties.put("id", id);
		properties.put("startTime", startTime);
		properties.put("endTime", endTime);
		properties.put("duration", duration);
		properties.put("length", length);
		properties.put("speed", speed);
		properties.put("medianRideSpeed", medianSpeed);
		properties.put("waitingTime", waitingTime);
		properties.put("rideId", ride.getId());
		return properties;
	}

}
