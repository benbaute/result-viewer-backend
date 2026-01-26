package com.simra.konsumgandalf.common.utils.services;

import com.simra.konsumgandalf.common.models.classes.RideLoc;
import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.repositories.TrafficSignalClusterRepository;
import com.simra.konsumgandalf.common.repositories.TrafficSignalRepository;
import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class OsmService {
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private CsvUtilService csvUtilService;

    @Autowired
    private FileReaderService fileReaderService;

    @Autowired
    private PlanetOsmLineRepository planetOsmLineRepository;

    @Autowired
    private TrafficSignalRepository trafficSignalRepository;

    @Autowired
    private TrafficSignalClusterRepository trafficSignalClusterRepository;

    @Transactional
    public int saveTrafficSignals(String filepath) throws IOException {
        List<TrafficSignal> trafficSignals = readOsmFile(filepath);

        Set<Long> existingIds = new HashSet<>(trafficSignalRepository.getTrafficSignalIds());
        List<TrafficSignal> newSignals = new ArrayList<>();

        for (TrafficSignal s : trafficSignals) {
            if (!existingIds.contains(s.getId())) {
                newSignals.add(s);
            }
        }
        trafficSignalRepository.saveAll(newSignals);

        return newSignals.size();
    }

    public List<TrafficSignal> readOsmFile(String filepath) throws IOException {
        InputStream input = new FileInputStream(filepath);
        OsmIterator iterator = new PbfIterator(input, true);
        Map<Long, List<PlanetOsmLine>> nodeToWays = new HashMap<>();
        List<TrafficSignal> trafficSignals = new ArrayList<>();
        for (EntityContainer container : iterator) {
            switch (container.getType()) {
                case Node:
                    OsmNode node = (OsmNode) container.getEntity();
                    for (int i = 0; i < node.getNumberOfTags(); i++) {
                        OsmTag tag = node.getTag(i);
                        if ("highway".equals(tag.getKey()) &&
                                "traffic_signals".equals(tag.getValue())) {
                            double lon = node.getLongitude();
                            double lat = node.getLatitude();
                            long id = node.getId();
                            Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                            TrafficSignal signal = new TrafficSignal(id, point);
                            trafficSignals.add(signal);
                        }
                    }
                    break;
                case Way:
                    OsmWay way = (OsmWay) container.getEntity();
                    long wayId = way.getId();
                    Optional<PlanetOsmLine> pLine = planetOsmLineRepository.findById(wayId);
                    if (pLine.isPresent()) {
                        for (int i = 0; i < way.getNumberOfNodes(); i++) {
                            long nodeId = way.getNodeId(i);
                            nodeToWays.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(pLine.get());
                        }
                    }
                    break;
                case Relation:
                    break;
            }
        }
        for (TrafficSignal signal : trafficSignals) {
            long signalId = signal.getId();
            List<PlanetOsmLine> ways = nodeToWays.get(signalId);

            if (ways != null) {
                for (PlanetOsmLine way : ways) {
                    signal.addOsmLine(way);
                }
            }
        }
        return trafficSignals;
    }

    private static List<List<Long>> parseListListLong(String raw) {
        List<List<Long>> result = new ArrayList<>();

        if (raw == null || raw.isEmpty()) return result;

        // Split by the group delimiter '|'
        String[] groups = raw.split("\\|");

        for (String group : groups) {
            List<Long> innerList = new ArrayList<>();
            // Split by the number delimiter ','
            for (String num : group.split(",")) {
                try {
                    innerList.add(Long.parseLong(num.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number found: " + num);
                }
            }
            result.add(innerList);
        }
        return result;
    }

    @Transactional
    public void createClusters() {
        trafficSignalClusterRepository.generateClusters();
    }

    @Transactional
    public void createClusterPolygons() {
        trafficSignalClusterRepository.updateClusterPolygons();
    }

    @Transactional
    public void populateClusterLineRelations() {
        trafficSignalClusterRepository.populateClusterLineRelations();
    }

    @Modifying
    @Transactional
    public int mergeClusters() throws IOException {
        Path baseDir = Paths.get("").toAbsolutePath();
        Path configFile = baseDir.resolve("common/src/main/resources/trafficSignal.config");
        FileInputStream input = new FileInputStream(configFile.toFile());
        String content = fileReaderService.readFileFromPath(configFile.toString());
        Properties properties = new Properties();
        properties.load(input);
        String clustersString = properties.getProperty("forced_clusters");

        int changedClusters = 0;
        List<List<Long>> list = parseListListLong(clustersString);
        for (List<Long> l : list) {
            List<TrafficSignalCluster> clusters = new ArrayList<>();
            for (Long id : l) {
                List<TrafficSignalCluster> c = trafficSignalClusterRepository.findByTrafficSignalId(id);
                for (TrafficSignalCluster current : c) {
                    boolean alreadyExists = false;
                    for (TrafficSignalCluster all : clusters) {
                        if (all.getId().equals(current.getId())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        clusters.add(current);
                    }
                }
            }
            if (clusters.size() > 1) {
                // merge clusters
                TrafficSignalCluster newCluster = new TrafficSignalCluster();
                Set<Long> ids = new HashSet<>();
                for (TrafficSignalCluster cluster : clusters) {
                    ids.addAll(cluster.getOriginalSignalIds());
                }
                newCluster.setOriginalSignalIds(ids.stream().toList());
                trafficSignalClusterRepository.deleteAll(clusters);
                trafficSignalClusterRepository.save(newCluster);
                changedClusters++;
            }
        }
        return changedClusters;
    }

    @Transactional
    public void updateNames() {
        List<TrafficSignalCluster> clusters = trafficSignalClusterRepository.findAll();
        for (TrafficSignalCluster cluster : clusters) {
            List<String> names = trafficSignalClusterRepository.getNames(cluster.getId());
            cluster.setOsmLinesName(names);
            trafficSignalClusterRepository.save(cluster);
        }
    }

    public List<TrafficSignal> getAllTrafficSignals() {
        return trafficSignalRepository.findAll();
    }

    public List<TrafficSignal> findTrafficSignalsByOsmLineId(Long osmLineId) { return trafficSignalRepository.findByOsmLineId(osmLineId); }

    public List<TrafficSignal> findTrafficSignalsByTrafficSignalClusterId(Long trafficSignalClusterId) {
        return trafficSignalRepository.findByTrafficSignalClusterId(trafficSignalClusterId);
    }

    public List<TrafficSignalCluster> findTrafficSignalClustersByTrafficSignalId(Long trafficSignalId) {
        return trafficSignalClusterRepository.findByTrafficSignalId(trafficSignalId);
    }

    public List<TrafficSignalCluster> findTrafficSignalClustersByTrafficSignalClusterId(Long trafficSignalClusterId) {
        return trafficSignalClusterRepository.findByTrafficSignalClusterId(trafficSignalClusterId);
    }

    public List<TrafficSignalCluster> findTrafficSignalClustersByOsmLineId(Long osmLineId) {
        return trafficSignalClusterRepository.findByOsmLineId(osmLineId);
    }

    public double getDistanceOsmLineTrafficSignal(Long osmLineId, Long trafficSignalId) {
        return trafficSignalRepository.getDistanceOsmLineTrafficSignal(osmLineId, trafficSignalId);
    }

    public List<TrafficSignalCluster> getAllTrafficSignalClusters() {
        return trafficSignalClusterRepository.findAll();
    }
}
