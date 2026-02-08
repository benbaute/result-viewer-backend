package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.entities.RideIncident;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.rides.services.RideIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("incidents")
public class RideIncidentController {

	@Autowired
	private RideIncidentService rideIncidentService;

	@GetMapping("street/{id}")
	public Map<String, List<RideIncident>> getIncidentsOfStreets(@PathVariable long id,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) int year) {
		List<RideIncident> incidents = rideIncidentService.getIncidentsOfStreetSegment(id, trafficTime, weekDay, year);
		return Map.of("incidents", incidents);
	}

	@GetMapping("marker")
	public Map<String, String> getIncidentsWithinRange() {
		return Map.of("incidents", rideIncidentService.getAllIncidentsWithRange());
	}

	@GetMapping("/{id}")
	public RideIncident getIncident(@PathVariable long id) {
		return rideIncidentService.getIncident(id);
	}

}
