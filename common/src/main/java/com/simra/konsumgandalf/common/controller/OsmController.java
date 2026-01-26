package com.simra.konsumgandalf.common.controller;

import com.simra.konsumgandalf.common.models.entities.MethodRun;
import com.simra.konsumgandalf.common.models.entities.RidePoint;
import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.common.utils.services.OsmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/osm")
public class OsmController {

	@Autowired
	private OsmService osmService;

    private final Path osmDirectory = Paths.get("postgis/data");

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

	@PostMapping("/traffic-signals/{file}") // berlin-latest.osm.pbf
	public ResponseEntity<?> saveTrafficSignals(@PathVariable String file) {
        Path filePath = osmDirectory.resolve(file);
        try {
            int saved = osmService.saveTrafficSignals(String.valueOf(filePath));
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Imported " + saved + " traffic signals.");
        }  catch (IOException e) {
            List<String> availableFiles = listAvailableFiles();
            return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "error", "File not found: " + file,
                    "availableFiles", availableFiles
            ));
        }
	}

    private List<String> listAvailableFiles() {
        try (Stream<Path> stream = Files.list(osmDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    @GetMapping("/traffic-signals")
    public ResponseEntity<Map<String, Object>> getAllTrafficSignals() {
        return ResponseEntity.ok(GeoService.getFeatureCollection(osmService.getAllTrafficSignals()));
    }

    @GetMapping("/traffic-signals/{osmLineId}")
    public ResponseEntity<Map<String, Object>> findTrafficSignalsByOsmLineId(@PathVariable Long osmLineId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(osmService.findTrafficSignalsByOsmLineId(osmLineId)));
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
