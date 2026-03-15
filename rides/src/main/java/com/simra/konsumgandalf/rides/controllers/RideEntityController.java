package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.rides.services.RideEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rides")
public class RideEntityController {

	@Autowired
	private RideEntityService rideEntityService;

	@GetMapping("geometries/{id}")
	public Map<String, String[]> getRideGeometries(@PathVariable long id) {
		return rideEntityService.getRideGeometries(id);
	}

}
