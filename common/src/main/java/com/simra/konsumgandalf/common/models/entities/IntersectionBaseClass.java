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
@MappedSuperclass
public abstract class IntersectionBaseClass extends TimeBaseClass {

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

    private double speed; // km/h

    private double duration; // s

    private double length; // m

    @Column
    private Double waitingTime; // s

    // For calculating region
    @Transient
    private Point startPoint;


	public IntersectionBaseClass() {
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
        properties.put("start_time", this.getStartTime());
        properties.put("end_time", this.getEndTime());
        properties.put("duration", this.getDuration());
        properties.put("length", this.getLength());
        properties.put("speed", this.getSpeed());
        properties.put("waiting_time", this.getWaitingTime());
        properties.put("ride_id", this.getRide().getId());
        properties.put("year", this.getYear());
        properties.put("day_of_week", this.getWeekDay());
        properties.put("traffic_time", this.getTrafficTime());
        return properties;
    }

    public abstract Set<Region> getRegions();
}
