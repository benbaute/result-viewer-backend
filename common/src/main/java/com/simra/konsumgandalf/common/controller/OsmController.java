package com.simra.konsumgandalf.common.controller;

import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.common.services.OsmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
    public ResponseEntity<Map<String, Object>> findTrafficSignalsByTrafficSignalClusterId(@PathVariable Long trafficSignalClusterId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(osmService.findTrafficSignalsByTrafficSignalClusterId(trafficSignalClusterId)));
    }

    @GetMapping("/cluster-polygons")
    public ResponseEntity<Map<String, Object>> getAllTrafficSignalClusterPolygons() {
        return ResponseEntity.ok(GeoService.getFeatureCollection(osmService.getAllTrafficSignalClusters()));
    }

    @GetMapping("/cluster-polygons/{trafficSignalClusterId}")
    public ResponseEntity<Map<String, Object>> getTrafficSignalClusterPolygon(@PathVariable Long trafficSignalClusterId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(
                osmService.findTrafficSignalClustersByTrafficSignalClusterId(trafficSignalClusterId)));
    }
}
