package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from intersection_node_metrics")
public class IntersectionNodeMetrics extends IntersectionBaseMetrics implements FeatureMappable {

	@Column(name = "start_valhalla_edge_id")
	private Long startValhallaEdgeId;

	@Column(name = "end_valhalla_edge_id")
	private Long endValhallaEdgeId;

	@Column(name = "start_osm_id")
	private Long startOsmId;

	@Column(name = "end_osm_id")
	private Long endOsmId;

	@Column(name = "traffic_signal_cluster_id")
	private Long trafficSignalClusterId;

	@Column(name = "start_name")
	private String startName;

	@Column(name = "end_name")
	private String endName;

	@Column(name = "street_names")
	private String streetNames;

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = this.getBaseProperties();
		properties.put("startValhallaEdgeId", startValhallaEdgeId);
		properties.put("endValhallaEdgeId", endValhallaEdgeId);
		properties.put("startOsmId", startOsmId);
		properties.put("endOsmId", endOsmId);
		properties.put("trafficSignalClusterId", trafficSignalClusterId);
		properties.put("startName", startName);
		properties.put("endName", endName);
		properties.put("streetNames", streetNames);
		return properties;
	}

}
