package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.rides.repositories.IntersectionEdgeMetricsRepository;
import com.simra.konsumgandalf.rides.repositories.IntersectionEdgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/intersections/intersection_edges")
@RequiredArgsConstructor
public class IntersectionEdgeController {

	private final IntersectionEdgeRepository intersectionEdgeRepository;

	private final IntersectionEdgeMetricsRepository intersectionEdgeMetricsRepository;

	@GetMapping("/{rideId}")
	public ResponseEntity<Map<String, Object>> getIntersectionEdgeAsGeoJson(@PathVariable Long rideId) {
		return ResponseEntity.ok(FeatureMappable.toFeatureCollection(intersectionEdgeRepository.findByRideId(rideId)));
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getIntersectionEdges(@RequestParam Boolean properties,
			@RequestParam(required = false) Long osmId, @RequestParam(required = false) Long valhallaEdgeId,
			@RequestParam(required = false) Long prevValhallaEdgeId,
			@RequestParam(required = false) Long nextValhallaEdgeId,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			@RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate,
			Pageable pageable) {
		Specification<IntersectionEdge> spec = intersectionEdgeRepository.createSpecification(osmId, valhallaEdgeId,
				prevValhallaEdgeId, nextValhallaEdgeId, regionLTreePath, trafficTime, weekDay, year,
				startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null, false);
		return ResponseEntity.ok(intersectionEdgeRepository.fetchPageCollection(spec, pageable, properties));
	}

	@GetMapping("/scroll")
	public ResponseEntity<Map<String, Object>> getIntersectionEdgesScroll(@RequestParam(required = false) Long osmId,
			@RequestParam(required = false) Long valhallaEdgeId,
			@RequestParam(required = false) Long prevValhallaEdgeId,
			@RequestParam(required = false) Long nextValhallaEdgeId,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			@RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate,
			@RequestParam Integer pageSize, @RequestParam(required = false) String lastId) {
		Specification<IntersectionEdge> spec = intersectionEdgeRepository.createSpecification(osmId, valhallaEdgeId,
				prevValhallaEdgeId, nextValhallaEdgeId, regionLTreePath, trafficTime, weekDay, year,
				startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null, false);
		return ResponseEntity.ok(intersectionEdgeRepository.fetchScrollCollection(spec, pageSize, lastId));
	}

	@GetMapping("/count")
	public ResponseEntity<Long> getIntersectionEdgesCount(@RequestParam(required = false) Long osmId,
			@RequestParam(required = false) Long valhallaEdgeId,
			@RequestParam(required = false) Long prevValhallaEdgeId,
			@RequestParam(required = false) Long nextValhallaEdgeId,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			@RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate) {
		Specification<IntersectionEdge> spec = intersectionEdgeRepository.createSpecification(osmId, valhallaEdgeId,
				prevValhallaEdgeId, nextValhallaEdgeId, regionLTreePath, trafficTime, weekDay, year,
				startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null, true);
		return ResponseEntity.ok(intersectionEdgeRepository.count(spec));
	}

	@GetMapping("/aggregate")
	public ResponseEntity<Map<String, Object>> getIntersectionEdgesAsGeoJsonPageable(@RequestParam Boolean properties,
			@RequestParam(required = false) Long osmId, @RequestParam(required = false) Long valhallaEdgeId,
			@RequestParam(required = false) Long prevValhallaEdgeId,
			@RequestParam(required = false) Long nextValhallaEdgeId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) String name,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, Pageable pageable) {
		Specification<IntersectionEdgeMetrics> spec = intersectionEdgeMetricsRepository.createSpecification(osmId,
				valhallaEdgeId, prevValhallaEdgeId, nextValhallaEdgeId, numberOfRides, regionLTreePath, name, weekDay,
				trafficTime, year);
		return ResponseEntity.ok(intersectionEdgeMetricsRepository.fetchPageCollection(spec, pageable, properties));
	}

	@GetMapping("/aggregate/scroll")
	public ResponseEntity<Map<String, Object>> getIntersectionEdgesAggregateScroll(
			@RequestParam(required = false) Long osmId, @RequestParam(required = false) Long valhallaEdgeId,
			@RequestParam(required = false) Long prevValhallaEdgeId,
			@RequestParam(required = false) Long nextValhallaEdgeId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) String name,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, @RequestParam Integer pageSize,
			@RequestParam(required = false) String lastId) {
		Specification<IntersectionEdgeMetrics> spec = intersectionEdgeMetricsRepository.createSpecification(osmId,
				valhallaEdgeId, prevValhallaEdgeId, nextValhallaEdgeId, numberOfRides, regionLTreePath, name, weekDay,
				trafficTime, year);
		return ResponseEntity.ok(intersectionEdgeMetricsRepository.fetchScrollCollection(spec, pageSize, lastId));
	}

	@GetMapping("/aggregate/count")
	public ResponseEntity<Long> getIntersectionEdgesAggregateCount(@RequestParam(required = false) Long osmId,
			@RequestParam(required = false) Long valhallaEdgeId,
			@RequestParam(required = false) Long prevValhallaEdgeId,
			@RequestParam(required = false) Long nextValhallaEdgeId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) String name,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year) {
		Specification<IntersectionEdgeMetrics> spec = intersectionEdgeMetricsRepository.createSpecification(osmId,
				valhallaEdgeId, prevValhallaEdgeId, nextValhallaEdgeId, numberOfRides, regionLTreePath, name, weekDay,
				trafficTime, year);
		return ResponseEntity.ok(intersectionEdgeMetricsRepository.count(spec));
	}

	@GetMapping("/streetNames")
	public List<String> getEdgeHighwayNames(@RequestParam(required = false) String regionLTreePath,
			@RequestParam(required = false) String name) {
		return intersectionEdgeRepository.findAllStreetNames(regionLTreePath, name);
	}

	@GetMapping(value = "/edge-metrics/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getEdgeMetricsTiles(@PathVariable int z, @PathVariable int x, @PathVariable int y,
			@RequestParam Long numberOfRides, @RequestParam String weekDay, @RequestParam String trafficTime,
			@RequestParam Integer year) {

		byte[] tile = intersectionEdgeMetricsRepository.getEdgeMetricsTile(z, x, y, numberOfRides, weekDay, trafficTime,
				year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

	@GetMapping(value = "/edge-metrics/start/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getEdgeMetricsStartTiles(@PathVariable int z, @PathVariable int x,
			@PathVariable int y, @RequestParam Long numberOfRides, @RequestParam String weekDay,
			@RequestParam String trafficTime, @RequestParam Integer year) {

		byte[] tile = intersectionEdgeMetricsRepository.getEdgeMetricsStartTile(z, x, y, numberOfRides, weekDay,
				trafficTime, year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

}
