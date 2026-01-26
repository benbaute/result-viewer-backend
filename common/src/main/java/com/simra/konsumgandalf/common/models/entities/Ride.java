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
import lombok.Getter;
import lombok.Setter;
import org.geolatte.geom.Geometry;
import java.util.*;

import static com.simra.konsumgandalf.common.constants.AppDates.FALLBACK_DATE_MILLIS;

/**
 * Contains metadata for a ride.
 */
@Getter
@Setter
@Entity
public class Ride {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String path;

	@Transient
	private List<RideLoc> rideLocations = new ArrayList<>();

	public Ride(String path) {
		this.path = path;
	}
	public Ride() {
	}
}
