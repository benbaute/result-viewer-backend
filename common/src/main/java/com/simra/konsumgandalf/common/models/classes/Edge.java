package com.simra.konsumgandalf.common.models.classes;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Edge {

	private Long valhallaEdgeId;

	private Long osmId;

	private PlanetOsmLine osmLine;

	private List<TrafficSignalCluster> trafficSignalClusters;

	public Edge() {
	}

}
