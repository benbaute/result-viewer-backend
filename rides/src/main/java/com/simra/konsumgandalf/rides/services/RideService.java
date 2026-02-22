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

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    RideService() {}


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

    public List<IntersectionNode> getIntersectionNodes(Long trafficSignalClusterId, Long startOsmId, Long endOsmId) {
        return intersectionNodeRepository.findByClusterIdStartEndOsmId(trafficSignalClusterId, startOsmId, endOsmId);
    }

    public List<IntersectionNodeMetrics> getIntersectionNodeMetricsComplete(
            Long numberOfRides, WeekDays weekDay, TrafficTimes trafficTime, Integer year) {
        return intersectionNodeMetricsRepository.getIntersectionNodeMetricsComplete(
                numberOfRides, weekDay.toString(), trafficTime.toString(), year);
    }

    public Map<String, Object> getIntersectionNodeMetricsPageable(
            Long trafficSignalClusterId, Long count, String region, String streetNames,
            WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

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

    public List<String> findAllStreetNamesIncludingStringIntersectionNode(Long trafficSignalClusterId,
              Long count, String region, String streetNames) {
        return intersectionNodeRepository.findAllIncludingString(trafficSignalClusterId, count, region, streetNames);
    }

    public List<IntersectionEdge> getIntersectionEdge(Long prevOsmId, Long osmId, Long nextOsmId) {
        return intersectionEdgeRepository.findByPrevIdOsmIdNext(prevOsmId, osmId, nextOsmId);
    }

    public List<IntersectionEdgeMetrics> getIntersectionEdgeMetricsComplete(
            Long numberOfRides, WeekDays weekDay, TrafficTimes trafficTime, Integer year) {
        return intersectionEdgeMetricsRepository.getIntersectionEdgeMetricsComplete(
                numberOfRides, weekDay.toString(), trafficTime.toString(), year);
    }

    public Map<String, Object> getIntersectionEdgeMetricsPageable(
            Long osmId, Long count, String region, String name,
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
        return intersectionBaseRepository.findRideIdByIntersectionBaseId(intersectionBaseId).
                map(List::of).orElse(Collections.emptyList());
    }

    public List<Long> findByOsmLineId(Long osmLineId) {
        return intersectionEdgeRepository.findByOsmLineId(osmLineId);
    }

    public List<IntersectionRegionMetrics> getIntersectionRegionMetricsComplete(
            Long numberOfRides, WeekDays weekDay, TrafficTimes trafficTime, Integer year) {
        return intersectionRegionMetricsRepository.getIntersectionRegionMetricsComplete(
                numberOfRides, weekDay.toString(), trafficTime.toString(), year);
    }

    public Map<String, Object> getIntersectionRegionMetricsPageable(
            Long regionId, Long count, WeekDays weekDay, TrafficTimes trafficTime, Integer year, Pageable pageable) {

        Specification<IntersectionRegionMetrics> spec = Specification
                .where(IntersectionRegionMetricsSpecifications.hasRegionId(regionId))
                .and(IntersectionBaseMetricsSpecifications.hasMinCount(count))
                .and(TimeSpecifications.hasWeekDay(weekDay))
                .and(TimeSpecifications.hasTrafficTime(trafficTime))
                .and(TimeSpecifications.hasYear(year));

        return GeoService.getFeatureCollection(intersectionRegionMetricsRepository.findAll(spec, pageable));
    }


    public List<? extends IntersectionBase> findIntersectionGroupById(Long id) {
        return intersectionBaseRepository.findById(id).map(this::fetchGroup).orElseGet(Collections::emptyList);
    }

    private List<? extends IntersectionBase> fetchGroup(IntersectionBase base) {
        return switch (base) {
            case IntersectionEdge edge -> intersectionEdgeRepository.findByPrevIdOsmIdNext(
                    edge.getPrevLine() != null ? edge.getPrevLine().getId() : null,
                    edge.getLine() != null ? edge.getLine().getId() : null,
                    edge.getNextLine() != null ? edge.getNextLine().getId() : null
            );
            case IntersectionNode node -> intersectionNodeRepository.findByClusterIdStartEndOsmId(
                    node.getTrafficSignalCluster().getId(),
                    node.getStartLine() != null ? node.getStartLine().getId() : null,
                    node.getEndLine() != null ? node.getEndLine().getId() : null
            );
            default -> Collections.emptyList();
        };
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

    @LogExecutionTime
    public void updateIntersectionMetrics() {
        intersectionNodeMetricsRepository.updateIntersectionNodeMetrics();
        intersectionEdgeMetricsRepository.updateIntersectionEdgeMetrics();
        intersectionRegionMetricsRepository.updateIntersectionRegionMetrics();
    }
}
