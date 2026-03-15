package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.classes.specifications.*;
import com.simra.konsumgandalf.rides.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RideService {

	@Autowired
	private RideRepository rideRepository;

	@Autowired
	private RidePointRepository ridePointRepository;

	@Autowired
	private MatchedPointRepository matchedPointRepository;

	@Autowired
	private IntersectionBaseRepository intersectionBaseRepository;

	@Autowired
	private IntersectionNodeRepository intersectionNodeRepository;

	@Autowired
	private IntersectionNodeMetricsRepository intersectionNodeMetricsRepository;

	@Autowired
	private IntersectionEdgeRepository intersectionEdgeRepository;

	@Autowired
	private IntersectionEdgeMetricsRepository intersectionEdgeMetricsRepository;

	@Autowired
	private IntersectionRegionMetricsRepository intersectionRegionMetricsRepository;

	@Autowired
	private IntersectionRideRegionMetricsRepository intersectionRideRegionMetricsRepository;

	RideService() {
	}

	public List<RidePoint> getRidePoints(Long rideId) {
		return ridePointRepository.findByRideId(rideId);
	}

	public List<MatchedPoint> getMatchedPoints(Long rideId) {
		return matchedPointRepository.findByRideId(rideId);
	}

	public List<IntersectionNode> getIntersectionNodes(Long rideId) {
		return intersectionNodeRepository.findByRideId(rideId);
	}

	public List<IntersectionEdge> getIntersectionEdge(Long rideId) {
		return intersectionEdgeRepository.findByRideId(rideId);
	}

	public List<IntersectionNode> getIntersectionNodesByClusterId(Long trafficSignalClusterId, TrafficTimes trafficTime,
			WeekDays weekDay, Integer year) {
		return intersectionNodeRepository.findAllByClusterId(trafficSignalClusterId, trafficTime, weekDay, year);
	}

	public List<IntersectionEdge> getIntersectionEdgesByOsmId(Long osmId, TrafficTimes trafficTime, WeekDays weekDay,
			Integer year) {
		return intersectionEdgeRepository.findAllByOsmId(osmId, trafficTime, weekDay, year);
	}

	public List<IntersectionNode> getIntersectionNodesByRegionId(Long regionId, TrafficTimes trafficTime,
			WeekDays weekDay, Integer year) {
		return intersectionNodeRepository.findAllByRegionId(regionId, trafficTime, weekDay, year);
	}

	public List<IntersectionEdge> getIntersectionEdgesByRegionId(Long regionId, TrafficTimes trafficTime,
			WeekDays weekDay, Integer year) {
		return intersectionEdgeRepository.findAllByRegionId(regionId, trafficTime, weekDay, year);
	}

	public List<IntersectionNodeMetrics> getIntersectionNodeMetricsComplete(Long numberOfRides, WeekDays weekDay,
			TrafficTimes trafficTime, Integer year) {
		return intersectionNodeMetricsRepository.getIntersectionNodeMetricsComplete(numberOfRides, weekDay.toString(),
				trafficTime.toString(), year);
	}

	public Map<String, Object> getIntersectionNodeMetricsPageable(Long trafficSignalClusterId, Long count,
			String region, String streetNames, WeekDays weekDay, TrafficTimes trafficTime, Integer year,
			Pageable pageable) {

		Specification<IntersectionNodeMetrics> spec = Specification
			.where(IntersectionNodeMetricsSpecifications.hasTrafficSignalClusterId(trafficSignalClusterId))
			.and(IntersectionNodeMetricsSpecifications.hasName(streetNames))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(count))
			.and(IntersectionBaseMetricsSpecifications.hasRegion(region))
			.and(TimeSpecifications.hasWeekDay(weekDay))
			.and(TimeSpecifications.hasTrafficTime(trafficTime))
			.and(TimeSpecifications.hasYear(year));

		return GeoService.getFeatureCollection(intersectionNodeMetricsRepository.findAll(spec, pageable));
	}

	public List<String> findAllStreetNamesIncludingStringIntersectionNode(Long trafficSignalClusterId, Long count,
			String region, String streetNames) {
		return intersectionNodeRepository.findAllIncludingString(trafficSignalClusterId, count, region, streetNames);
	}

	public List<IntersectionEdgeMetrics> getIntersectionEdgeMetricsComplete(Long numberOfRides, WeekDays weekDay,
			TrafficTimes trafficTime, Integer year) {
		return intersectionEdgeMetricsRepository.getIntersectionEdgeMetricsComplete(numberOfRides, weekDay.toString(),
				trafficTime.toString(), year);
	}

	public Map<String, Object> getIntersectionEdgeMetricsPageable(Long osmId, Long count, String region, String name,
			WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

		Specification<IntersectionEdgeMetrics> spec = Specification
			.where(IntersectionEdgeMetricsSpecifications.hasOsmId(osmId))
			.and(IntersectionEdgeMetricsSpecifications.hasName(name))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(count))
			.and(IntersectionBaseMetricsSpecifications.hasRegion(region))
			.and(TimeSpecifications.hasWeekDay(weekDay))
			.and(TimeSpecifications.hasTrafficTime(trafficTime))
			.and(TimeSpecifications.hasYear(year));

		return GeoService.getFeatureCollection(intersectionEdgeMetricsRepository.findAll(spec, pageable));
	}

	public List<String> findAllStreetNamesIntersectionEdge(Long count, String region, String name) {
		return intersectionEdgeRepository.findAllStreetNames(count, region, name);
	}

	public List<Long> getRideIds() {
		return rideRepository.getRideIds();
	}

	public List<Long> getRideIdByIntersectionBaseId(Long intersectionBaseId) {
		return intersectionBaseRepository.findRideIdByIntersectionBaseId(intersectionBaseId)
			.map(List::of)
			.orElse(Collections.emptyList());
	}

	public List<Long> findByOsmLineId(Long osmLineId) {
		return intersectionEdgeRepository.findByOsmLineId(osmLineId);
	}

	public List<IntersectionRegionMetrics> getIntersectionRegionMetricsComplete(Long numberOfRides, WeekDays weekDay,
			TrafficTimes trafficTime, Integer year) {
		return intersectionRegionMetricsRepository.getIntersectionRegionMetricsComplete(numberOfRides,
				weekDay.toString(), trafficTime.toString(), year);
	}

	public Map<String, Object> getIntersectionRegionMetricsPageable(Long regionId, Integer adminLevel,
			Long numberOfRides, WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

		Specification<IntersectionRegionMetrics> spec = Specification
			.where(IntersectionRegionMetricsSpecifications.hasRegionId(regionId))
			.and(IntersectionRegionMetricsSpecifications.hasAdminLevel(adminLevel))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(numberOfRides))
			.and(TimeSpecifications.hasWeekDay(weekDay))
			.and(TimeSpecifications.hasTrafficTime(trafficTime))
			.and(TimeSpecifications.hasYear(year));

		return GeoService.getFeatureCollection(intersectionRegionMetricsRepository.findAll(spec, pageable));
	}

	public List<IntersectionRideRegionMetrics> getIntersectionRideRegionMetrics(Long regionId, WeekDays weekDay,
			TrafficTimes trafficTime, Integer year) {
		return intersectionRideRegionMetricsRepository.getIntersectionRideRegionMetrics(regionId, weekDay.toString(),
				trafficTime.toString(), year);
	}

	public Optional<IntersectionBase> getIntersectionBase(Long intersectionBaseId) {
		return intersectionBaseRepository.findById(intersectionBaseId);
	}

	public List<? extends IntersectionBase> getIntersectionBase(Long id, TrafficTimes trafficTime, WeekDays weekDay,
			Integer year, Date startDate, Date endDate) {
		return intersectionBaseRepository.findById(id)
			.map(b -> fetchGroup(b, trafficTime, weekDay, year, startDate, endDate))
			.orElseGet(Collections::emptyList);
	}

	private List<? extends IntersectionBase> fetchGroup(IntersectionBase base, TrafficTimes trafficTime,
			WeekDays weekDay, Integer year, Date startDate, Date endDate) {
		switch (base) {
			case IntersectionEdge edge -> {
				Long prev = edge.getPrevValhallaEdgeId();
				Long id = edge.getValhallaEdgeId();
				Long next = edge.getNextValhallaEdgeId();
				if (startDate != null && endDate != null) {
					return intersectionEdgeRepository.findByGroupValhallaEdgeId(prev, id, next, startDate, endDate);
				}
				else if (trafficTime != null && weekDay != null && year != null) {
					return intersectionEdgeRepository.findByGroupValhallaEdgeId(prev, id, next, trafficTime, weekDay,
							year);
				}
				return Collections.emptyList();
			}
			case IntersectionNode node -> {
				Long signalId = node.getTrafficSignalCluster().getId();
				Long startId = node.getStartValhallaEdgeId();
				Long endId = node.getEndValhallaEdgeId();
				if (startDate != null && endDate != null) {
					return intersectionNodeRepository.findByClusterIdGroupValhallaEdgeId(signalId, startId, endId,
							startDate, endDate);
				}
				else if (trafficTime != null && weekDay != null && year != null) {
					return intersectionNodeRepository.findByClusterIdGroupValhallaEdgeId(signalId, startId, endId,
							trafficTime, weekDay, year);
				}
				return Collections.emptyList();
			}
			default -> {
				return Collections.emptyList();
			}
		}
	}

	public List<MatchedPoint> getMatchedPointsByBaseId(Long id) {
		IntersectionBase base = intersectionBaseRepository.findById(id).orElse(null);
		if (base == null) {
			return Collections.emptyList();
		}
		List<MatchedPoint> matchedPoints = base.getMatchedPoints();
		if (base.getPrevIntersection() != null) {
			matchedPoints.addAll(base.getPrevIntersection().getMatchedPoints());
		}
		if (base.getNextIntersection() != null) {
			matchedPoints.addAll(base.getNextIntersection().getMatchedPoints());
		}
		return matchedPoints;
	}

	public List<RidePoint> getRidePointsByBaseId(Long id) {
		return getMatchedPointsByBaseId(id).stream().map(MatchedPoint::getRidePoint).toList();
	}

	public byte[] getNodeMetricsTile(int z, int x, int y, Long numberOfRides, String weekDay, String trafficTime,
			int year) {
		return intersectionNodeMetricsRepository.getNodeMetricsTile(z, x, y, numberOfRides, weekDay, trafficTime, year);
	}

	public byte[] getNodeMetricsStartTile(int z, int x, int y, Long numberOfRides, String weekDay, String trafficTime,
			int year) {
		return intersectionNodeMetricsRepository.getNodeMetricsStartTile(z, x, y, numberOfRides, weekDay, trafficTime,
				year);
	}

	public byte[] getEdgeMetricsTile(int z, int x, int y, Long numberOfRides, String weekDay, String trafficTime,
			int year) {
		return intersectionEdgeMetricsRepository.getEdgeMetricsTile(z, x, y, numberOfRides, weekDay, trafficTime, year);
	}

	public byte[] getEdgeMetricsStartTile(int z, int x, int y, Long numberOfRides, String weekDay, String trafficTime,
			int year) {
		return intersectionEdgeMetricsRepository.getEdgeMetricsStartTile(z, x, y, numberOfRides, weekDay, trafficTime,
				year);
	}

	@LogExecutionTime
	public void updateIntersectionMetrics() {
		intersectionNodeMetricsRepository.updateIntersectionNodeMetrics();
		intersectionEdgeMetricsRepository.updateIntersectionEdgeMetrics();
		intersectionRegionMetricsRepository.updateIntersectionRegionMetrics();
		intersectionRideRegionMetricsRepository.updateIntersectionRideRegionMetrics();
	}

}
