package com.simra.konsumgandalf.osmPlanet.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.classes.PageResult;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsPlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsRegion;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsSimraRegion;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricRegionDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricsLineDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricsRegionDTO;
import com.simra.konsumgandalf.osmPlanet.classes.specifications.SafetyMetricsGenericSpecification;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsPlanetOsmLineRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsRegionRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsSimraRegionRepository;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SafetyMetricsService {

	@Autowired
	private SafetyMetricsPlanetOsmLineRepository safetyMetricsPlanetOsmLineRepository;

	@Autowired
	private SafetyMetricsRegionRepository safetyMetricsRegionRepository;

	@Autowired
	private SafetyMetricsSimraRegionRepository safetyMetricsSimraRegionRepository;

	@Autowired
	private RegionRepository regionRepository;

	public Optional<SafetyMetricsPlanetOsmLine> getSafetyMetricsOfStreet(long id, TrafficTimes trafficTime,
			WeekDays weekDay, int year) {
		return safetyMetricsPlanetOsmLineRepository.findByStreetId(id, trafficTime, weekDay, year);
	}

	@Cacheable(value = "streetsMetrics",
			key = "T(java.util.Objects).hash(#p0, #p1, #p2, #p3, #p4, #p5, #p6, #p7, #p8, #p9, #p10, #pageable.pageNumber, #pageable.pageSize, #pageable.sort)")
	public PageResult<SafetyMetricsLineDTO> getFilteredData(Long id, String name, List<String> highwayType,
			Float minDangerousScore, Float maxDangerousScore, Integer minNumberOfRides, Integer minNumberOfIncidents,
			List<TrafficTimes> trafficTime, List<WeekDays> weekDay, List<Integer> year, String regionName,
			Pageable pageable) {

		Optional<Geometry> regionWayOpt = regionRepository.findRegionWayByName(regionName);
		Geometry regionWay = regionWayOpt.orElse(null);

		Specification<SafetyMetricsPlanetOsmLine> spec = SafetyMetricsGenericSpecification.filterBy("planetOsmLine", id,
				name, highwayType, minDangerousScore, maxDangerousScore, minNumberOfRides, minNumberOfIncidents,
				trafficTime, weekDay, year, regionWay);

		Page<SafetyMetricsLineDTO> page = safetyMetricsPlanetOsmLineRepository.findAll(spec, pageable)
			.map(safetyMetrics -> new SafetyMetricsLineDTO(safetyMetrics.getPlanetOsmLine().getId(),
					safetyMetrics.getPlanetOsmLine().getName(), safetyMetrics.getPlanetOsmLine().getHighway(),
					safetyMetrics.getDangerousScore(), safetyMetrics.getDangerousColor(),
					safetyMetrics.getNumberOfRides(), safetyMetrics.getNumberOfIncidents(),
					safetyMetrics.getTrafficTime(), safetyMetrics.getWeekDay(), safetyMetrics.getYear()));
		return new PageResult<>(page.getContent(), pageable.getPageNumber(), pageable.getPageSize(),
				page.getTotalElements());
	}

	@Cacheable(value = "regionMetrics",
			key = "T(java.util.Objects).hash(#p0, #p1, #p2, #p3, #p4, #p5, #p6, #p7, #p8, #p9, #pageable.pageNumber, #pageable.pageSize, #pageable.sort)")
	public PageResult<SafetyMetricsRegionDTO> getRegionMetrics(String name, Float minDangerousScore,
			Integer minNumberOfRides, Integer minNumberOfIncidents, Integer adminLevel, List<TrafficTimes> trafficTime,
			List<WeekDays> weekDay, List<Integer> year, Pageable pageable) {
		Specification<SafetyMetricsRegion> spec = SafetyMetricsGenericSpecification.filterBy("region", name,
				minDangerousScore, minNumberOfRides, minNumberOfIncidents, adminLevel, trafficTime, weekDay, year);

		Page<SafetyMetricsRegionDTO> page = safetyMetricsRegionRepository.findAll(spec, pageable)
			.map(safetyMetrics -> new SafetyMetricsRegionDTO(safetyMetrics.getRegion().getName(),
					safetyMetrics.getDangerousScore(), safetyMetrics.getDangerousColor(),
					safetyMetrics.getNumberOfRides(), safetyMetrics.getNumberOfIncidents(),
					safetyMetrics.getTrafficTime(), safetyMetrics.getWeekDay(), safetyMetrics.getYear()));
		return new PageResult<>(page.getContent(), pageable.getPageNumber(), pageable.getPageSize(),
				page.getTotalElements());
	}

	@Cacheable(value = "simraRegionMetrics",
			key = "T(java.util.Objects).hash(#p0, #p1, #p2, #p3, #p4, #p5, #p6, #p7, #p8, #pageable.pageNumber, #pageable.pageSize, #pageable.sort)")
	public PageResult<SafetyMetricsRegionDTO> getSimraRegionMetrics(String name, Float minDangerousScore,
			Integer minNumberOfRides, Integer minNumberOfIncidents, List<TrafficTimes> trafficTime,
			List<WeekDays> weekDay, List<Integer> year, Pageable pageable) {
		Specification<SafetyMetricsSimraRegion> spec = SafetyMetricsGenericSpecification.filterBy("region", name,
				minDangerousScore, minNumberOfRides, minNumberOfIncidents, trafficTime, weekDay, year);

		Page<SafetyMetricsRegionDTO> page = safetyMetricsSimraRegionRepository.findAll(spec, pageable)
			.map(safetyMetrics -> new SafetyMetricsRegionDTO(safetyMetrics.getRegion().getName(),
					safetyMetrics.getDangerousScore(), safetyMetrics.getDangerousColor(),
					safetyMetrics.getNumberOfRides(), safetyMetrics.getNumberOfIncidents(),
					safetyMetrics.getTrafficTime(), safetyMetrics.getWeekDay(), safetyMetrics.getYear()));
		return new PageResult<>(page.getContent(), pageable.getPageNumber(), pageable.getPageSize(),
				page.getTotalElements());
	}

	public List<SafetyMetricsRegion> getRegionSafetyMetrics(String name) {
		return safetyMetricsRegionRepository.findByName(name);
	}

	public List<SafetyMetricsSimraRegion> getSimraRegionSafetyMetrics(String name) {
		return safetyMetricsSimraRegionRepository.findByName(name);
	}

	@Cacheable(value = "getMetricsForHighways", key = "T(java.util.Objects).hash(#p0, #p1, #p2)")
	public Map<String, String> getMetricsForHighways(TrafficTimes trafficTimes, WeekDays weekDay, int year) {
		return safetyMetricsPlanetOsmLineRepository.getFilteredSafetyMetrics(trafficTimes, weekDay, year)
			.stream()
			.collect(Collectors.toMap(SafetyMetricDTO::getOsmId, SafetyMetricDTO::getDangerousColor));
	}

	@Cacheable(value = "getMetricsForRegions", key = "T(java.util.Objects).hash(#p0, #p1, #p2)")
	public Map<String, String> getMetricsForRegions(TrafficTimes trafficTimes, WeekDays weekDay, int year) {
		return safetyMetricsRegionRepository.getFilteredSafetyMetrics(trafficTimes, weekDay, year)
			.stream()
			.collect(Collectors.toMap(SafetyMetricRegionDTO::getName, SafetyMetricRegionDTO::getDangerousColor));
	}

	@LogExecutionTime
	public void updateSafetyMetrics() {
		safetyMetricsPlanetOsmLineRepository.updateSafetyMetricsPlanetOsmLine();
		safetyMetricsRegionRepository.updateSafetyMetricsRegion();
		safetyMetricsSimraRegionRepository.updateSafetyMetricsSimraRegion();
	}

}
