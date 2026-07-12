package com.simra.konsumgandalf.osmPlanet.controller;

import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.osmPlanet.services.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/regions")
public class RegionController {

	@Autowired
	private RegionService regionService;

	@GetMapping("/{name}")
	public Optional<Region> getRegionGeometry(@PathVariable String name) {
		return regionService.getRegionByName(name);
	}

	@GetMapping("name")
	public List<String> getAllRegions() {
		return regionService.getAllRegions("");
	}

	@GetMapping("regionTree")
	public List<Map<String, Object>> getRegionTree() {
		return regionService.getRegionTree();
	}

	@GetMapping("name/{prefix}")
	public List<String> getAllRegions(@PathVariable String prefix) {
		return regionService.getAllRegions(prefix);
	}

	@GetMapping("/map")
	public void exportPolygonJson() throws IOException {
		regionService.exportPolygonJson();
	}

}
