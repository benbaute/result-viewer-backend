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
import java.util.*;

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

	public List<Map<String, Object>> getRegionTree() {
		List<Region> allRegions = regionRepository.findAllWithRidesOrderByAdminLevel();
		Map<Long, Map<String, Object>> nodeMap = new HashMap<>();
		List<Map<String, Object>> rootNodes = new ArrayList<>();

		for (Region region : allRegions) {
			Map<String, Object> node = region.getProperties();
			nodeMap.put(region.getId(), node);

			if (region.getParent() == null) {
				rootNodes.add(node);
			}
			else {
				Map<String, Object> parentNode = nodeMap.get(region.getParent().getId());
				if (parentNode != null) {
					if (parentNode.containsKey("children")) {
						((List<Map<String, Object>>) parentNode.get("children")).add(node);
					}
					else {
						List<Map<String, Object>> children = new ArrayList<>();
						children.add(node);
						parentNode.put("children", children);
					}
				}
			}
		}
		return rootNodes;
	}

	public void saveRegions() {
		regionRepository.saveRegions();
		regionRepository.updateRegionHierarchy();
		regionRepository.updateLtreePaths();
	}

	public boolean emptyRegions() {
		return regionRepository.count() == 0;
	}

}
