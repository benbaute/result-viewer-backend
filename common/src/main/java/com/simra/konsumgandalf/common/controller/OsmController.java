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

    @PostMapping("/cluster")
    public ResponseEntity<?> generateClusters() {
        osmService.createClusters();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge-cluster")
    public ResponseEntity<?> mergeCluster() {
        try {
            int changed = osmService.mergeClusters();
            return ResponseEntity.ok().body(Map.of("merged", changed));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getStackTrace()));
        }
    }

    @PostMapping("/cluster-names")
    public ResponseEntity<?> generateClusterNames() {
        osmService.updateNames();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cluster-polygons")
    public ResponseEntity<?> generateClusterPolygons() {
        osmService.createClusterPolygons();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cluster-polygons-osm-lines")
    public ResponseEntity<?> populateClusterLineRelations() {
        osmService.populateClusterLineRelations();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cluster-complete")
    public ResponseEntity<?> generateClusterComplete() {
        generateClusters();
        mergeCluster();
        generateClusterNames();
        generateClusterPolygons();
        populateClusterLineRelations();
        return ResponseEntity.ok().build();
    }

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
