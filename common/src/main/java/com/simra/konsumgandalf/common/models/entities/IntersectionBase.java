package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.enums.WeekDays;
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
public abstract class IntersectionBase extends TimeBaseClass {

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
    private double duration; // s

    @Column
    private double length; // m

    @Column
    private Double waitingTime; // s

    @ManyToMany
    @JoinTable(
            name = "intersection__region",
            joinColumns = @JoinColumn(name = "intersection_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id")
    )
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
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", getId());
        properties.put("startTime", this.getStartTime());
        properties.put("endTime", this.getEndTime());
        properties.put("duration", this.getDuration());
        properties.put("length", this.getLength());
        properties.put("speed", this.getSpeed());
        properties.put("waitingTime", this.getWaitingTime());
        properties.put("rideId", this.getRide().getId());
        properties.put("year", this.getYear());
        properties.put("dayOfWeek", this.getWeekDay());
        properties.put("trafficTime", this.getTrafficTime());
        return properties;
    }
}
