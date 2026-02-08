package com.simra.konsumgandalf.osmPlanet.controller;

import com.simra.konsumgandalf.common.models.classes.PageResult;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsPlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsRegion;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsSimraRegion;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricsLineDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricsRegionDTO;
import com.simra.konsumgandalf.osmPlanet.services.SafetyMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("safety-metrics")
public class SafetyMetricsController {

	@Autowired
	private SafetyMetricsService safetyMetricsService;

	@GetMapping("streets/{id}")
	public Optional<SafetyMetricsPlanetOsmLine> getSafetyDetailsOfStreet(@PathVariable long id,
			@RequestParam(defaultValue = "ALL_DAY") TrafficTimes trafficTime,
			@RequestParam(defaultValue = "ALL_WEEK") WeekDays weekDay, @RequestParam(defaultValue = "2000") int year) {
		return safetyMetricsService.getSafetyMetricsOfStreet(id, trafficTime, weekDay, year);
	}

	@GetMapping("/streets")
	public PageResult<SafetyMetricsLineDTO> getFilteredData(@RequestParam(required = false) Long id,
			@RequestParam(required = false) String name, @RequestParam(required = false) List<String> highway,
			@RequestParam(required = false) Float minDangerousScore,
			@RequestParam(required = false) Float maxDangerousScore,
			@RequestParam(required = false) Integer minNumberOfRides,
			@RequestParam(required = false) Integer minNumberOfIncidents,
			@RequestParam(required = false) List<TrafficTimes> trafficTime,
			@RequestParam(required = false) List<WeekDays> weekDay, @RequestParam(required = false) List<Integer> year,
			@RequestParam(required = false) String region, Pageable pageable) {
		return safetyMetricsService.getFilteredData(id, name, highway, minDangerousScore, maxDangerousScore,
				minNumberOfRides, minNumberOfIncidents, trafficTime, weekDay, year, region, pageable);
	}

	@GetMapping("/regions")
	public PageResult<SafetyMetricsRegionDTO> getRegionMetrics(@RequestParam(required = false) String name,
			@RequestParam(required = false) Float minDangerousScore,
			@RequestParam(required = false) Integer minNumberOfRides,
			@RequestParam(required = false) Integer minNumberOfIncidents,
			@RequestParam(required = false) Integer adminLevel,
			@RequestParam(required = false) List<TrafficTimes> trafficTime,
			@RequestParam(required = false) List<WeekDays> weekDay, @RequestParam(required = false) List<Integer> year,
			Pageable pageable) {
		return safetyMetricsService.getRegionMetrics(name, minDangerousScore, minNumberOfRides, minNumberOfIncidents,
				adminLevel, trafficTime, weekDay, year, pageable);
	}

	@GetMapping("/regions/{name}")
	public List<SafetyMetricsRegion> getRegionSafetyMetrics(@PathVariable String name) {
		return safetyMetricsService.getRegionSafetyMetrics(name);
	}

	@GetMapping("/simra-regions")
	public PageResult<SafetyMetricsRegionDTO> getSimraRegionMetrics(@RequestParam(required = false) String name,
			@RequestParam(required = false) Float minDangerousScore,
			@RequestParam(required = false) Integer minNumberOfRides,
			@RequestParam(required = false) Integer minNumberOfIncidents,
			@RequestParam(required = false) List<TrafficTimes> trafficTime,
			@RequestParam(required = false) List<WeekDays> weekDay, @RequestParam(required = false) List<Integer> year,
			Pageable pageable) {
		return safetyMetricsService.getSimraRegionMetrics(name, minDangerousScore, minNumberOfRides,
				minNumberOfIncidents, trafficTime, weekDay, year, pageable);
	}

	@GetMapping("/simra-regions/{name}")
	public List<SafetyMetricsSimraRegion> getSimraRegionSafetyMetrics(@PathVariable String name) {
		return safetyMetricsService.getSimraRegionSafetyMetrics(name);
	}

	@GetMapping("/streets-grid")
	public Map<String, String> getSafetyMetrics(@RequestParam TrafficTimes trafficTime, @RequestParam WeekDays weekDay,
			@RequestParam int year) {
		return safetyMetricsService.getMetricsForHighways(trafficTime, weekDay, year);
	}

	@GetMapping("/region-map")
	public Map<String, String> getRegionSafetyMetrics(@RequestParam TrafficTimes trafficTime,
			@RequestParam WeekDays weekDay, @RequestParam int year) {
		return safetyMetricsService.getMetricsForRegions(trafficTime, weekDay, year);
	}

}
