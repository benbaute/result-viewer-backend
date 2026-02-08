package com.simra.konsumgandalf.osmPlanet.controller;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityDTO;
import com.simra.konsumgandalf.osmPlanet.services.OsmHighwayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("streets")
@Validated
public class StreetsController {

	@Autowired
	private OsmHighwayService osmHighwayService;

	@GetMapping("/grid")
	public List<Map<String, Object>> getHighwayInformation(@RequestParam double lat, @RequestParam double lng,
			@RequestParam int zoom, @RequestParam(defaultValue = "ALL_DAY") TrafficTimes trafficTime,
			@RequestParam(defaultValue = "ALL_WEEK") WeekDays weekDay, @RequestParam(defaultValue = "2000") int year) {
		return osmHighwayService.getHighwayInformation(lat, lng, zoom, trafficTime, weekDay, year);
	}

	@GetMapping("/name/{name-prefix}")
	public List<String> getHighwayNames(@PathVariable("name-prefix") String namePrefix) {
		return osmHighwayService.findAllHighwayNameStartingWith(namePrefix);
	}

	@GetMapping("/id/{id-prefix}")
	public List<String> getHighwayIds(@PathVariable("id-prefix") String idPrefix) {
		return osmHighwayService.findAllHighwayIdStartingWith(idPrefix);
	}

	@GetMapping("/{id}")
	public Optional<PlanetOsmLine> getHighwayById(@PathVariable("id") long id) {
		return osmHighwayService.getHighwayById(id);
	}

	@GetMapping("/{id}/ride-entities")
	public Map<String, List<RideEntityDTO>> getRideEntitiesTimeById(@PathVariable("id") long id,
			@RequestParam("rideStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime rideStart,
			@RequestParam("rideEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime rideEnd) {

		return Map.of("rides", osmHighwayService.getRideEntitiesTimeById(id, rideStart, rideEnd));
	}

	@GetMapping("/map")
	public void exportGridJson() throws IOException {
		osmHighwayService.exportGridJson();
	}

}
