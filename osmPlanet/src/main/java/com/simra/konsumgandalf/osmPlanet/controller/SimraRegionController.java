package com.simra.konsumgandalf.osmPlanet.controller;

import com.simra.konsumgandalf.common.models.entities.SimraRegion;
import com.simra.konsumgandalf.osmPlanet.services.SimraRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simra-regions")
public class SimraRegionController {

	@Autowired
	private SimraRegionService simraRegionService;

	@GetMapping("/{name}")
	public SimraRegion getRegionGeometry(@PathVariable String name) {
		return simraRegionService.getRegionByName(name);
	}

}
