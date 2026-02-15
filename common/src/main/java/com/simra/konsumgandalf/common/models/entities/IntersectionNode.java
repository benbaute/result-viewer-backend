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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_osm_id")
    private PlanetOsmLine startLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_osm_id")
    private PlanetOsmLine endLine;

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
        PlanetOsmLine startLine = this.getStartLine();
        PlanetOsmLine endLine = this.getEndLine();
        properties.put("startOsmId", startLine != null ? startLine.getId() : null);
        properties.put("endOsmId", endLine != null ? endLine.getId() : null);
        properties.put("startName", startLine != null ? startLine.getName() : null);
        properties.put("endName", endLine != null ? endLine.getName() : null);
        properties.put("streetNames", this.getStreetNames());
        properties.put("trafficSignalClusterId", this.trafficSignalCluster.getId());
        return properties;
    }
}
