package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
@Entity
public class IntersectionNode extends IntersectionBase implements FeatureMappable {

    private Long startValhallaEdgeId;
    private Long endValhallaEdgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_osm_id")
    private PlanetOsmLine startOsmLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_osm_id")
    private PlanetOsmLine endOsmLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traffic_signal_cluster_id")
    private TrafficSignalCluster trafficSignalCluster;

    private String streetNames;

	// --- Constructor ---
	public IntersectionNode() {
	}

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        properties.put("startValhallaEdgeId", startValhallaEdgeId);
        properties.put("endValhallaEdgeId", endValhallaEdgeId);
        properties.put("startOsmId", startOsmLine != null ? startOsmLine.getId() : null);
        properties.put("endOsmId", endOsmLine != null ? endOsmLine.getId() : null);
        properties.put("startName", startOsmLine != null ? startOsmLine.getName() : null);
        properties.put("endName", endOsmLine != null ? endOsmLine.getName() : null);
        properties.put("streetNames", streetNames);
        properties.put("trafficSignalClusterId", trafficSignalCluster.getId());
        return properties;
    }
}
