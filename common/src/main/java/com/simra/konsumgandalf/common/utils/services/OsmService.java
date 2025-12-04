package com.simra.konsumgandalf.common.utils.services;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class OsmService {
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private TrafficSignalRepository trafficSignalRepository;

    @Autowired
    private TrafficSignalClusterRepository trafficSignalClusterRepository;

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
        Map<Long, List<Long>> nodeToWays = new HashMap<>();
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
                    for (int i = 0; i < way.getNumberOfNodes(); i++) {
                        long nodeId = way.getNodeId(i);
                        nodeToWays.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(wayId);
                    }
                    break;
                case Relation:
                    break;
            }
        }
        for (TrafficSignal signal : trafficSignals) {
            long signalId = signal.getId();
            List<Long> wayIds = nodeToWays.get(signalId);
        }
        return trafficSignals;
    }

    @Transactional
    public void createClusters() {
        trafficSignalClusterRepository.generateClusters();
    }

    @Transactional
    public void populateLineSignalRelations() {
        trafficSignalRepository.populateLineSignalRelations();
    }

    public List<TrafficSignal> getAllTrafficSignals() {
        return trafficSignalRepository.findAll();
    }

    public List<TrafficSignal> findTrafficSignalsByOsmLineId(Long osmLineId) { return trafficSignalRepository.findByOsmLineId(osmLineId); }

    public double getDistanceOsmLineTrafficSignal(Long osmLineId, Long trafficSignalId) {
        return trafficSignalRepository.getDistanceOsmLineTrafficSignal(osmLineId, trafficSignalId);
    }

    public List<TrafficSignalCluster> getAllTrafficSignalClusters() {
        return trafficSignalClusterRepository.findAll();
    }
}
