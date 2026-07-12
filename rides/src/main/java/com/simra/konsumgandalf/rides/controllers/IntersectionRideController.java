package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.entities.MatchedPoint;
import com.simra.konsumgandalf.common.models.entities.Ride;
import com.simra.konsumgandalf.common.models.entities.RidePoint;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import com.simra.konsumgandalf.rides.repositories.IntersectionBaseRepository;
import com.simra.konsumgandalf.rides.repositories.MatchedPointRepository;
import com.simra.konsumgandalf.rides.repositories.RidePointRepository;
import com.simra.konsumgandalf.rides.repositories.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/intersections/ride")
@RequiredArgsConstructor
public class IntersectionRideController {

	private final RideRepository rideRepository;

	private final RidePointRepository ridePointRepository;

	private final MatchedPointRepository matchedPointRepository;

	private final IntersectionBaseRepository intersectionBaseRepository;

	@GetMapping("/points/{rideId}")
	public ResponseEntity<Map<String, Object>> getRidePointsAsGeoJson(@PathVariable Long rideId) {
		return ResponseEntity.ok(FeatureMappable.toFeatureCollection(ridePointRepository.findByRideId(rideId)));
	}

	@GetMapping("/matched_points/{rideId}")
	public ResponseEntity<Map<String, Object>> getMatchedPointsAsGeoJson(@PathVariable Long rideId) {
		return ResponseEntity.ok(FeatureMappable.toFeatureCollection(matchedPointRepository.findByRideId(rideId)));
	}

	@GetMapping("/intersection_base/{intersectionBaseId}")
	public ResponseEntity<Map<String, Object>> getIntersectionBaseAsGeoJson(@PathVariable Long intersectionBaseId) {
		return intersectionBaseRepository.findById(intersectionBaseId)
			.map(base -> ResponseEntity.ok(FeatureMappable.toFeatureCollection(List.of(base))))
			.orElseGet(() -> ResponseEntity.ok(null));
	}

	@GetMapping("/matched_points_and_ride_points")
	public ResponseEntity<Map<String, Object>> getMatchedPointsByBaseIdAsGeoJson(@RequestParam Long id) {
		List<MatchedPoint> matchedPoints = matchedPointRepository.findMatchedPointsForIntersectionAndNeighbors(id);
		List<RidePoint> ridePoints = matchedPoints.stream().map(MatchedPoint::getRidePoint).toList();

		Map<String, Object> response = new HashMap<>();
		response.put("matchedPoints", FeatureMappable.toFeatureCollection(matchedPoints));
		response.put("ridePoints", FeatureMappable.toFeatureCollection(ridePoints));

		return ResponseEntity.ok(response);
	}

	@GetMapping("/ids")
	public ResponseEntity<Map<String, Object>> getRideIds(@RequestParam(required = false) Long id, Pageable pageable) {
		Specification<Ride> spec = rideRepository.createSpecification(id);
		Page<Ride> rides = rideRepository.fetchPage(spec, pageable);
		return ResponseEntity
			.ok(PropertiesMappable.toPageableMap(rides, "ids", rides.getContent().stream().map(Ride::getId).toList()));
	}

}
