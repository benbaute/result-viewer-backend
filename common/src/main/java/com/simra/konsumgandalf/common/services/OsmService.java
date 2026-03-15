package com.simra.konsumgandalf.common.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import com.simra.konsumgandalf.common.repositories.TrafficSignalClusterRepository;
import com.simra.konsumgandalf.common.repositories.TrafficSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class OsmService {

	private static final Logger _logger = LoggerFactory.getLogger(OsmService.class);

	@Autowired
	private TrafficSignalRepository trafficSignalRepository;

	@Autowired
	private TrafficSignalClusterRepository trafficSignalClusterRepository;

	@Transactional
	public void saveTrafficSignals() {
		trafficSignalRepository.saveTrafficSignals();
	}

	@Transactional
	public void setSignalIdsOnCluster() {
		trafficSignalClusterRepository.setSignalIdsOnCluster();
	}

	@Transactional
	public void createClusterPolygons() {
		trafficSignalClusterRepository.setClusterGeometry();
		trafficSignalClusterRepository.setClusterGeometry3857();
	}

	@Transactional
	public void populateClusterLineRelations() {
		trafficSignalClusterRepository.populateClusterLineRelations();
	}

	private static List<List<Long>> parseListListLong(String raw) throws IOException {
		List<List<Long>> result = new ArrayList<>();

		if (raw == null || raw.isEmpty())
			return result;

		// Split by the group delimiter '|'
		String[] groups = raw.split("\\|");

		for (String group : groups) {
			List<Long> innerList = new ArrayList<>();
			// Split by the number delimiter ','
			for (String num : group.split(",")) {
				try {
					innerList.add(Long.parseLong(num.trim()));
				}
				catch (NumberFormatException e) {
					throw new IOException("Invalid number found: " + num);
				}
			}
			result.add(innerList);
		}
		return result;
	}

	@Modifying
	@Transactional
	public void mergeClusters() throws IOException {
		Path baseDir = Paths.get("").toAbsolutePath();
		Path configFile = baseDir.resolve("common/src/main/resources/trafficSignal.config");
		FileInputStream input = new FileInputStream(configFile.toFile());
		Properties properties = new Properties();
		properties.load(input);
		String clustersString = properties.getProperty("forced_clusters");

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
			}
		}
	}

	@Transactional
	public void setStreetNames() {
		trafficSignalClusterRepository.setStreetNames();
	}

	@Transactional
	public void saveTrafficSignalClusters() {
		setSignalIdsOnCluster();
		try {
			mergeClusters();
		}
		catch (IOException e) {
			_logger.error("Failed to merge clusters.", e);
		}
		createClusterPolygons();
		populateClusterLineRelations();
		setStreetNames();
	}

	@LogExecutionTime
	@Transactional
	public void loadTrafficSignalData() {
		saveTrafficSignals();
		saveTrafficSignalClusters();
	}

	public List<TrafficSignal> getAllTrafficSignals() {
		return trafficSignalRepository.findAll();
	}

	public List<TrafficSignal> findTrafficSignalsByTrafficSignalClusterId(Long trafficSignalClusterId) {
		return trafficSignalRepository.findByTrafficSignalClusterId(trafficSignalClusterId);
	}

	public List<TrafficSignalCluster> findTrafficSignalClustersByTrafficSignalClusterId(Long trafficSignalClusterId) {
		return trafficSignalClusterRepository.findByTrafficSignalClusterId(trafficSignalClusterId);
	}

	@LogExecutionTimeSubTask
	public List<Object[]> findTrafficSignalClustersByOsmLineIds(List<Long> osmLineIds) {
		return trafficSignalClusterRepository.findByOsmLineIds(osmLineIds);
	}

	public List<TrafficSignalCluster> getAllTrafficSignalClusters() {
		return trafficSignalClusterRepository.findAll();
	}

	public byte[] getClusterTile(int z, int x, int y) {
		return trafficSignalClusterRepository.getClusterTile(z, x, y);
	}

	public byte[] getSignalTile(int z, int x, int y) {
		return trafficSignalRepository.getSignalTile(z, x, y);
	}

	public boolean emptyTrafficSignals() {
		return trafficSignalRepository.count() == 0;
	}

}
