package com.simra.konsumgandalf.common.controller;

import com.simra.konsumgandalf.common.models.entities.MethodRun;
import com.simra.konsumgandalf.common.models.entities.RidePoint;
import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
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
        List<TrafficSignal> trafficSignals = osmService.getAllTrafficSignals();

        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features",
                trafficSignals.stream()
                        .map(t -> Map.of("type", "Feature", "geometry",
                                Map.of("type", "Point", "coordinates", List.of(t.getGeom().getX(), t.getGeom().getY())),
                                "properties", Map.of("id", t.getId())))
                        .toList());

        return ResponseEntity.ok(geoJson);
    }

    @GetMapping("/traffic-signals/{osmLineId}")
    public ResponseEntity<Map<String, Object>> findTrafficSignalsByOsmLineId(@PathVariable Long osmLineId) {
        List<TrafficSignal> trafficSignals = osmService.findTrafficSignalsByOsmLineId(osmLineId);

        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features",
                trafficSignals.stream()
                        .map(t -> Map.of("type", "Feature", "geometry",
                                Map.of("type", "Point", "coordinates", List.of(t.getGeom().getX(), t.getGeom().getY())),
                                "properties", Map.of("id", t.getId())))
                        .toList());

        return ResponseEntity.ok(geoJson);
    }

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> getAllTrafficSignalClusters() {
        List<TrafficSignalCluster> trafficSignalClusters = osmService.getAllTrafficSignalClusters();

        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features",
                trafficSignalClusters.stream()
                        .map(t -> Map.of("type", "Feature", "geometry",
                                Map.of("type", "Point", "coordinates", List.of(t.getGeom().getX(), t.getGeom().getY())),
                                "properties", Map.of("id", t.getId(),
                                        "originalIds", t.getOriginalSignalIds())))
                        .toList());

        return ResponseEntity.ok(geoJson);
    }

    @GetMapping("/cluster-polygons")
    public ResponseEntity<Map<String, Object>> getAllTrafficSignalClusterPolygons() {
        List<TrafficSignalCluster> trafficSignalClusters = osmService.getAllTrafficSignalClusters();

        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features",
                trafficSignalClusters.stream()
                        .map(t -> Map.of("type", "Feature", "geometry",
                                t.getPolygon(),
                                "properties", Map.of("id", t.getId(),
                                        "originalIds", t.getOriginalSignalIds())))
                        .toList());

        return ResponseEntity.ok(geoJson);
    }
}
