package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.classes.RideLocation;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.maps.TrafficTimesMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

import java.util.*;

import static com.simra.konsumgandalf.common.constants.AppDates.FALLBACK_DATE_MILLIS;

/**
 * This class is the root entity for all OSM objects.
 */
@Getter
@Setter
@Entity
public class RideEntity extends TimeBaseClass {

	private static final Calendar calendar = new GregorianCalendar();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	private Date rideStart;

	@Temporal(TemporalType.TIMESTAMP)
	private Date rideEnd;

	@Column(columnDefinition = "geometry(LineString,4326)")
	private LineString way;

	@Transient
	private List<RideLocation> rideLocations = new ArrayList<>();

	@Transient
	private ArrayList<MatchInformation> cleanLocations = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ride_entity_id", referencedColumnName = "id")
	private List<RideIncident> rideIncidents = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "ride_entity__planet_osm_line")
	private List<PlanetOsmLine> planetOsmLines = new ArrayList<>();

	@Column(columnDefinition = "text")
	private String coordinates;

	@Column(unique = true)
	private String path;

	public RideEntity() {
	}

	public RideEntity(String path) {
		this.path = path;
	}

	@PrePersist
	private void calculateTrafficTimesAndWeekDays() {
		if (rideStart == null || rideEnd == null) {
			return;
		}

		Date rideMedianDate = new Date((rideStart.getTime() + rideEnd.getTime()) / 2);

		TrafficTimes trafficTime;
		WeekDays weekDay;
		int year;

		if (rideStart.getTime() == FALLBACK_DATE_MILLIS || rideEnd.getTime() == FALLBACK_DATE_MILLIS) {
			trafficTime = TrafficTimes.ALL_DAY;
			weekDay = WeekDays.ALL_WEEK;
			year = 2000;
		}
		else {
			trafficTime = TrafficTimesMapper.getTrafficTime(rideMedianDate);
			calendar.setTime(rideMedianDate);

			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

			weekDay = dayOfWeek <= 5 ? WeekDays.WEEK : WeekDays.WEEKEND;
			year = calendar.get(Calendar.YEAR);
		}

		super.setYear(year);
		super.setWeekDay(weekDay);
		super.setTrafficTime(trafficTime);

		for (RideIncident rideIncident : rideIncidents) {
			rideIncident.setTrafficTime(trafficTime);
			rideIncident.setWeekDay(weekDay);
			rideIncident.setYear(year);
		}
	}

}
