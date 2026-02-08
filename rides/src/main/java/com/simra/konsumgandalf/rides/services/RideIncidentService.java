package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.models.entities.RideIncident;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.rides.models.specifications.RideIncidentSpecification;
import com.simra.konsumgandalf.rides.repositories.RideIncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RideIncidentService {

	@Autowired
	private RideIncidentRepository rideIncidentRepository;

	public List<RideIncident> getIncidentsOfStreetSegment(long id, TrafficTimes trafficTime, WeekDays weekDay,
			int year) {
		return this.rideIncidentRepository.findAll(RideIncidentSpecification.filterBy(id, trafficTime, weekDay, year));
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private List<Map<String, Object>> getAllIncidentsWithRange(double lat, double lon, double radius, int limit) {
		return this.rideIncidentRepository.getAllIncidentsWithRange(lat, lon, radius);
	}

	@Cacheable(value = "incidentsWithinRange")
	public String getAllIncidentsWithRange() {
		String sql = """
				SELECT json_agg(
					JSON_BUILD_OBJECT(
						'id', ride_incident.id,
						'lng', ride_incident.lng,
						'lat', ride_incident.lat,
						'scary', ride_incident.scary
					)
				   			) FROM ride_incident
				  """;

		return jdbcTemplate.queryForObject(sql, String.class);
	}

	public RideIncident getIncident(long id) {
		return this.rideIncidentRepository.findById(id).orElse(null);
	}

}
