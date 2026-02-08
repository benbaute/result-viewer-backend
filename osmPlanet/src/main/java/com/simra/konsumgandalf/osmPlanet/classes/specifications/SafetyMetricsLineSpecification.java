package com.simra.konsumgandalf.osmPlanet.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsPlanetOsmLine;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SafetyMetricsLineSpecification {

	public static Specification<SafetyMetricsPlanetOsmLine> filterBy(Long id, String name, List<String> highwayType,
			Float minDangerousScore, Float maxDangerousScore, Integer minNumberOfRides, Integer minNumberOfIncidents,
			List<TrafficTimes> trafficTime, List<WeekDays> weekDay, List<Integer> year) {

		return (Root<SafetyMetricsPlanetOsmLine> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			Join<SafetyMetricsPlanetOsmLine, PlanetOsmLine> planetOsmLineJoin = root.join("planetOsmLine");

			if (id != null) {
				predicates.add(cb.like(planetOsmLineJoin.get("id").as(String.class), id + "%"));

			}
			if (highwayType != null && !highwayType.isEmpty()) {
				predicates.add(planetOsmLineJoin.get("highway").in(highwayType));
			}
			if (name != null) {
				predicates.add(cb.like(planetOsmLineJoin.get("name"), "%" + name + "%"));
			}
			if (minDangerousScore != null && maxDangerousScore != null) {
				predicates.add(cb.between(root.get("dangerousScore"), minDangerousScore, maxDangerousScore));
			}
			else if (minDangerousScore != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("dangerousScore"), minDangerousScore));
			}
			else if (maxDangerousScore != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("dangerousScore"), maxDangerousScore));
			}
			if (minNumberOfRides != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("numberOfRides"), minNumberOfRides));
			}
			if (minNumberOfIncidents != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("numberOfIncidents"), minNumberOfIncidents));
			}
			if (trafficTime != null) {
				predicates.add(root.get("trafficTime").in(trafficTime));
			}
			if (weekDay != null) {
				predicates.add(root.get("weekDay").in(weekDay));
			}
			if (year != null) {
				predicates.add(root.get("year").in(year));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

}
