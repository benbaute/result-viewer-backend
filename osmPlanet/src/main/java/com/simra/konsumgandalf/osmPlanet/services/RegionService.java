package com.simra.konsumgandalf.osmPlanet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RegionService {

	private final String exportPath;

	@Autowired
	private RegionRepository regionRepository;

	RegionService(@Value("${SIMRA_EXPORT_VOLUME_PATH}") String exportPath) {
		this.exportPath = exportPath;
	}

	public Optional<Region> getRegionByName(String name) {
		return regionRepository.findBasicRegionByName(name);
	}

	public List<String> getAllRegions(String prefix) {
		return regionRepository.findAllNames(prefix);
	}

	@LogExecutionTime
	public void exportPolygonJson() throws IOException {
		List<Map<String, Object>> json = regionRepository.getPolygonRaw();
		ObjectMapper mapper = new ObjectMapper();
		File target = new File(exportPath + "/region-map.json");
		mapper.writeValue(target, json);
	}

	public void saveRegions() {
		regionRepository.saveRegions();
	}

	public boolean emptyRegions() {
		return regionRepository.count() == 0;
	}

}
