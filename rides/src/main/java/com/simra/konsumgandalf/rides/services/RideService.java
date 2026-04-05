package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.classes.specifications.*;
import com.simra.konsumgandalf.rides.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

	public Page<IntersectionNode> getIntersectionNodesPageable(Long trafficSignalClusterId, Long startValhallaEdgeId,
			Long endValhallaEdgeId, Long regionId, TrafficTimes trafficTime, WeekDays weekDay, Integer year,
			Date startDate, Date endDate, Pageable pageable) {

		Specification<IntersectionNode> spec = Specification
			.where(IntersectionNodeSpecifications.hasTrafficSignalClusterId(trafficSignalClusterId))
			.and(IntersectionNodeSpecifications.isSegment(startValhallaEdgeId, endValhallaEdgeId))
			.and(IntersectionBaseSpecifications.hasRegion(regionId))
			.and(TimeSpecifications.hasWeekDayBase(weekDay))
			.and(TimeSpecifications.hasTrafficTimeBase(trafficTime))
			.and(TimeSpecifications.hasYearBase(year))
			.and(TimeSpecifications.inDateRange(startDate, endDate));
		return intersectionNodeRepository.findAll(spec, pageable);
	}

	public Page<IntersectionEdge> getIntersectionEdgesPageable(Long osmId, Long valhallaEdgeId, Long prevValhallaEdgeId,
			Long nextValhallaEdgeId, Long regionId, TrafficTimes trafficTime, WeekDays weekDay, Integer year,
			Date startDate, Date endDate, Pageable pageable) {
		Specification<IntersectionEdge> spec = Specification.where(IntersectionEdgeSpecifications.hasOsmId(osmId))
			.and(IntersectionEdgeSpecifications.isSegment(valhallaEdgeId, prevValhallaEdgeId, nextValhallaEdgeId))
			.and(IntersectionBaseSpecifications.hasRegion(regionId))
			.and(TimeSpecifications.hasWeekDayBase(weekDay))
			.and(TimeSpecifications.hasTrafficTimeBase(trafficTime))
			.and(TimeSpecifications.hasYearBase(year))
			.and(TimeSpecifications.inDateRange(startDate, endDate));
		return intersectionEdgeRepository.findAll(spec, pageable);
	}

	public Page<IntersectionNodeMetrics> getIntersectionNodeMetricsPageable(Long trafficSignalClusterId,
			Long startValhallaEdgeId, Long endValhallaEdgeId, Long count, String region, Long regionId,
			String streetNames, WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

		Specification<IntersectionNodeMetrics> spec = Specification
			.where(IntersectionNodeMetricsSpecifications.hasTrafficSignalClusterId(trafficSignalClusterId))
			.and(IntersectionNodeMetricsSpecifications.isSegment(startValhallaEdgeId, endValhallaEdgeId))
			.and(IntersectionNodeMetricsSpecifications.hasName(streetNames))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(count))
			.and(IntersectionBaseMetricsSpecifications.hasRegion(region))
			.and(IntersectionBaseMetricsSpecifications.hasRegionId(regionId))
			.and(TimeSpecifications.hasWeekDayMetrics(weekDay))
			.and(TimeSpecifications.hasTrafficTimeMetrics(trafficTime))
			.and(TimeSpecifications.hasYearMetrics(year));

		return intersectionNodeMetricsRepository.findAll(spec, pageable);
	}

	public List<String> findAllStreetNamesIncludingStringIntersectionNode(Long trafficSignalClusterId, Long count,
			String region, String streetNames) {
		return intersectionNodeRepository.findAllIncludingString(trafficSignalClusterId, count, region, streetNames);
	}

	public Page<IntersectionEdgeMetrics> getIntersectionEdgeMetricsPageable(Long osmId, Long valhallaEdgeId,
			Long prevValhallaEdgeId, Long nextValhallaEdgeId, Long count, String region, Long regionId, String name,
			WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

		Specification<IntersectionEdgeMetrics> spec = Specification
			.where(IntersectionEdgeMetricsSpecifications.hasOsmId(osmId))
			.and(IntersectionEdgeMetricsSpecifications.isSegment(valhallaEdgeId, prevValhallaEdgeId,
					nextValhallaEdgeId))
			.and(IntersectionEdgeMetricsSpecifications.hasName(name))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(count))
			.and(IntersectionBaseMetricsSpecifications.hasRegion(region))
			.and(IntersectionBaseMetricsSpecifications.hasRegionId(regionId))
			.and(TimeSpecifications.hasWeekDayMetrics(weekDay))
			.and(TimeSpecifications.hasTrafficTimeMetrics(trafficTime))
			.and(TimeSpecifications.hasYearMetrics(year));

		return intersectionEdgeMetricsRepository.findAll(spec, pageable);
	}

	public List<String> findAllStreetNamesIntersectionEdge(Long count, String region, String name) {
		return intersectionEdgeRepository.findAllStreetNames(count, region, name);
	}

	public Map<String, Object> getRideIdsPageable(Long id, Pageable pageable) {
		Specification<Ride> spec = Specification.where(RideSpecifications.hasIdLike(id));
		Page<Ride> pages = rideRepository.findAll(spec, pageable);
		return GeoService.getPageableMap(pages, "ids", pages.getContent().stream().map(Ride::getId).toList());
	}

	public List<IntersectionRegionMetrics> getIntersectionRegionMetricsComplete(Long numberOfRides, Integer adminLevel,
			WeekDays weekDay, TrafficTimes trafficTime, Integer year) {
		return intersectionRegionMetricsRepository.getIntersectionRegionMetricsComplete(numberOfRides, adminLevel,
				weekDay.toString(), trafficTime.toString(), year);
	}

	public Page<IntersectionRegionMetrics> getIntersectionRegionMetricsPageable(Long regionId, Integer adminLevel,
			Long numberOfRides, WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

		Specification<IntersectionRegionMetrics> spec = Specification
			.where(IntersectionRegionMetricsSpecifications.hasRegionId(regionId))
			.and(IntersectionRegionMetricsSpecifications.hasAdminLevel(adminLevel))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(numberOfRides))
			.and(TimeSpecifications.hasWeekDayMetrics(weekDay))
			.and(TimeSpecifications.hasTrafficTimeMetrics(trafficTime))
			.and(TimeSpecifications.hasYearMetrics(year));

		return intersectionRegionMetricsRepository.findAll(spec, pageable);
	}

	public Page<IntersectionRideRegionMetrics> getIntersectionRideRegionMetricsPageable(Long regionId, WeekDays weekDay,
			TrafficTimes trafficTime, Integer year, Pageable pageable) {

		Specification<IntersectionRideRegionMetrics> spec = Specification
			.where(IntersectionRideRegionMetricsSpecifications.hasRegionId(regionId))
			.and(TimeSpecifications.hasWeekDayBase(weekDay))
			.and(TimeSpecifications.hasTrafficTimeBase(trafficTime))
			.and(TimeSpecifications.hasYearBase(year));

		return intersectionRideRegionMetricsRepository.findAll(spec, pageable);
	}

	public Optional<IntersectionBase> getIntersectionBase(Long intersectionBaseId) {
		return intersectionBaseRepository.findById(intersectionBaseId);
	}

	public List<MatchedPoint> getMatchedPointsByBaseId(Long id) {
		IntersectionBase base = intersectionBaseRepository.findById(id).orElse(null);
		List<MatchedPoint> matchedPoints = matchedPointRepository.findByIntersectionBaseId(id);
		if (base != null && base.getPrevIntersectionId() != null) {
			matchedPoints.addAll(matchedPointRepository.findByIntersectionBaseId(base.getPrevIntersectionId()));
		}
		Optional<IntersectionBase> nextIntersection = intersectionBaseRepository.findByPrevIntersectionId(id);
		nextIntersection.ifPresent(intersectionBase -> matchedPoints
			.addAll((matchedPointRepository.findByIntersectionBaseId(intersectionBase.getId()))));
		return matchedPoints;
	}

	public List<RidePoint> getRidePointsByBaseId(Long id) {
		IntersectionBase base = intersectionBaseRepository.findById(id).orElse(null);
		List<RidePoint> ridePoints = ridePointRepository.findByIntersectionBaseId(id);
		if (base != null && base.getPrevIntersectionId() != null) {
			ridePoints.addAll(ridePointRepository.findByIntersectionBaseId(base.getPrevIntersectionId()));
		}
		Optional<IntersectionBase> nextIntersection = intersectionBaseRepository.findByPrevIntersectionId(id);
		nextIntersection.ifPresent(intersectionBase -> ridePoints
			.addAll((ridePointRepository.findByIntersectionBaseId(intersectionBase.getId()))));
		return ridePoints;
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
