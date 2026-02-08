package com.simra.konsumgandalf.osmPlanet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityDTO;
import com.simra.konsumgandalf.osmPlanet.classes.enums.RoadTypes;
import com.simra.konsumgandalf.osmPlanet.classes.mapper.ZoomDistanceMapper;
import com.simra.konsumgandalf.osmPlanet.classes.mapper.ZoomRoadTypeMapper;
import com.simra.konsumgandalf.osmPlanet.repositories.OsmHighwayRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OsmHighwayService {

	private final ZoomDistanceMapper zoomDistanceMapper = new ZoomDistanceMapper();

	private final ZoomRoadTypeMapper zoomRoadTypeMapper = new ZoomRoadTypeMapper();

	private final String exportPath;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private OsmHighwayRepository osmHighwayRepository;

	OsmHighwayService(@Value("${SIMRA_EXPORT_VOLUME_PATH}") String exportPath) {
		this.exportPath = exportPath;
	}

	public List<Map<String, Object>> getHighwayInformation(double lat, double lng, int zoom, TrafficTimes trafficTime,
			WeekDays weekDay, int year) {
		int distanceFilter = zoomDistanceMapper.getDistanceForZoom(zoom);
		List<String> roadTypes = zoomRoadTypeMapper.getRoadTypes(zoom).stream().map(RoadTypes::getType).toList();

		if (zoom <= 11) {
			int adminLevel = zoom <= 9 ? 4 : 6;

			return regionRepository.findWays(adminLevel, lng, lat, distanceFilter, 0.0001, trafficTime.name(),
					weekDay.name(), year);
		}

		return osmHighwayRepository.findHighways(lng, lat, distanceFilter, roadTypes, 0.0001, trafficTime.name(),
				weekDay.name(), year);
	}

	public Optional<PlanetOsmLine> getHighwayById(long id) {
		return osmHighwayRepository.findById(id);
	}

	public List<RideEntityDTO> getRideEntitiesTimeById(long id, LocalDateTime startTime, LocalDateTime endTime) {
		return osmHighwayRepository.findRideEntitiesTimeById(id, startTime, endTime);
	}

	public List<String> findAllHighwayNameStartingWith(String namePrefix) {
		return osmHighwayRepository.findAllHighwayNameStartingWith(namePrefix);
	}

	public List<String> findAllHighwayIdStartingWith(String idPrefix) {
		return osmHighwayRepository.findAllHighwayIdStartingWith(idPrefix);
	}

    @LogExecutionTime
	public void exportGridJson() throws IOException {
		List<Map<String, Object>> json = osmHighwayRepository.getGridRaw();
		ObjectMapper mapper = new ObjectMapper();
		File target = new File(exportPath + "/street-map.json");
		mapper.writeValue(target, json);
	}
}
