package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.entities.IntersectionRegionMetrics;
import com.simra.konsumgandalf.common.models.entities.IntersectionRideRegionMetrics;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.rides.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/intersections/regions")
@RequiredArgsConstructor
public class IntersectionRegionController {

	private final RideRepository rideRepository;

	private final RidePointRepository ridePointRepository;

	private final MatchedPointRepository matchedPointRepository;

	private final IntersectionBaseRepository intersectionBaseRepository;

	private final IntersectionNodeRepository intersectionNodeRepository;

	private final IntersectionNodeMetricsRepository intersectionNodeMetricsRepository;

	private final IntersectionEdgeRepository intersectionEdgeRepository;

	private final IntersectionEdgeMetricsRepository intersectionEdgeMetricsRepository;

	private final IntersectionRegionMetricsRepository intersectionRegionMetricsRepository;

	private final IntersectionRideRegionMetricsRepository intersectionRideRegionMetricsRepository;

	@GetMapping("/complete")
	public ResponseEntity<Map<String, Object>> getIntersectionRegionMetricsComplete(@RequestParam Integer adminLevel,
			@RequestParam Long numberOfRides, @RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime,
			@RequestParam Integer year) {
		return ResponseEntity.ok(FeatureMappable.toFeatureCollection(intersectionRegionMetricsRepository
			.getIntersectionRegionMetricsComplete(numberOfRides, adminLevel, weekDay, trafficTime, year)));
	}

	@GetMapping("/pageable")
	public ResponseEntity<Map<String, Object>> getIntersectionRegionMetricsPageable(@RequestParam Boolean properties,
			@RequestParam(required = false) Long regionId, @RequestParam(required = false) String regionLTreePath,
			@RequestParam(required = false) Integer adminLevel, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, Pageable pageable) {
		Specification<IntersectionRegionMetrics> spec = intersectionRegionMetricsRepository.createSpecification(
				regionId, regionLTreePath, adminLevel, numberOfRides, weekDay, trafficTime, year, pageable);
		return ResponseEntity.ok(intersectionRegionMetricsRepository.fetchPageCollection(spec, pageable, properties));
	}

	@GetMapping("/rides/scroll")
	public ResponseEntity<Map<String, Object>> getIntersectionRideRegionMetricsScroll(@RequestParam Long regionId,
			@RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime, @RequestParam Integer year,
			@RequestParam Integer pageSize, @RequestParam(required = false) String lastId) {
		Specification<IntersectionRideRegionMetrics> spec = intersectionRideRegionMetricsRepository
			.createSpecification(regionId, weekDay, trafficTime, year);
		return ResponseEntity.ok(intersectionRideRegionMetricsRepository.fetchScrollCollection(spec, pageSize, lastId));
	}

	@GetMapping("/rides/count")
	public ResponseEntity<Long> getIntersectionRideRegionMetricsCount(@RequestParam Long regionId,
			@RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime, @RequestParam Integer year) {
		Specification<IntersectionRideRegionMetrics> spec = intersectionRideRegionMetricsRepository
			.createSpecification(regionId, weekDay, trafficTime, year);
		return ResponseEntity.ok(intersectionRideRegionMetricsRepository.count(spec));
	}

}
