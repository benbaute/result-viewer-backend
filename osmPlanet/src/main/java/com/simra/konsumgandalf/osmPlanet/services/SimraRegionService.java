package com.simra.konsumgandalf.osmPlanet.services;

import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.common.models.entities.SimraRegion;
import com.simra.konsumgandalf.osmPlanet.classes.mapper.SimraRegionMapper;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsSimraRegionRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SimraRegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimraRegionService {
    @Autowired
    private RegionRepository regionRepository;

	@Autowired
	private SimraRegionRepository simraRegionRepository;

	@Autowired
	private SafetyMetricsSimraRegionRepository safetyMetricsSimraRegionRepository;

    private static final Logger _logger = LoggerFactory.getLogger(SimraRegionService.class);
    private static final SimraRegionMapper simraMapper = new SimraRegionMapper();


    public SimraRegion getRegionByName(String name) {
        Optional<SimraRegion> simraRegion = simraRegionRepository.findWayByName(name);
		return simraRegion.orElse(null);
	}

    public void createOrUpdateSimraRegions() {
        Map<String, List<String>> simraRegionMap = new HashMap<>(simraMapper.map);
        simraRegionMap.put("All",
                regionRepository.findAll()
                        .stream()
                        .filter(region -> region.getAdminLevel() == 4)
                        .map(Region::getName)
                        .toList());
        List<SimraRegion> simraRegions = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : simraRegionMap.entrySet()) {
            String simraRegionName = entry.getKey();
            SimraRegion simraRegion = simraRegionRepository.findByName(simraRegionName)
                    .orElseGet(SimraRegion::new);
            simraRegion.setName(simraRegionName);
            List<Region> regions = new ArrayList<>();
            for (String regionString : entry.getValue()) {
                Optional<Region> optionalRegion = regionRepository.findByName(regionString);
                if (optionalRegion.isEmpty()) {
                    continue;
                }
                Region region = optionalRegion.get();
                regions.add(region);
            }
            if (regions.isEmpty()) {
                _logger.info("No regions found for simra region {}", simraRegionName);
                continue;
            }

            simraRegion.setRegions(regions);
            simraRegions.add(simraRegion);
        }

        simraRegionRepository.saveAll(simraRegions);
        simraRegionRepository.setSimraRegionGeometry();
    }
}
