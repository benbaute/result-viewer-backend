package com.simra.konsumgandalf.common.controller;

import com.simra.konsumgandalf.common.services.OsmService;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/osm")
public class OsmController {

	@Autowired
	private OsmService osmService;

	@GetMapping("/traffic-signals")
	public ResponseEntity<Map<String, Object>> getAllTrafficSignals() {
		return ResponseEntity.ok(GeoService.getFeatureCollection(osmService.getAllTrafficSignals()));
	}

	@GetMapping("/traffic-signals/cluster/{trafficSignalClusterId}")
	public ResponseEntity<Map<String, Object>> findTrafficSignalsByTrafficSignalClusterId(
			@PathVariable Long trafficSignalClusterId) {
		return ResponseEntity.ok(GeoService
			.getFeatureCollection(osmService.findTrafficSignalsByTrafficSignalClusterId(trafficSignalClusterId)));
	}

	@GetMapping("/cluster-polygons")
	public ResponseEntity<Map<String, Object>> getAllTrafficSignalClusterPolygons() {
		return ResponseEntity.ok(GeoService.getFeatureCollection(osmService.getAllTrafficSignalClusters()));
	}

	@GetMapping("/cluster-polygons/{trafficSignalClusterId}")
	public ResponseEntity<Map<String, Object>> getTrafficSignalClusterPolygon(
			@PathVariable Long trafficSignalClusterId) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(
				osmService.findTrafficSignalClustersByTrafficSignalClusterId(trafficSignalClusterId)));
	}

	@GetMapping(value = "/cluster/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getClusterTiles(@PathVariable int z, @PathVariable int x, @PathVariable int y) {

		byte[] tile = osmService.getClusterTile(z, x, y);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

	@GetMapping(value = "/signal/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getSignalTiles(@PathVariable int z, @PathVariable int x, @PathVariable int y) {

		byte[] tile = osmService.getSignalTile(z, x, y);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

}
