package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.classes.IntersectionEdgeMetricsIDKey;
import com.simra.konsumgandalf.common.models.classes.IntersectionNodeMetricsIDKey;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;

import java.util.Map;

@Getter
@Entity
@IdClass(IntersectionNodeMetricsIDKey.class)
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from intersection_node_metrics")
public class IntersectionNodeMetrics extends IntersectionBaseClassMetrics implements FeatureMappable {

    @Id
    @Column(name = "start_osm_id")
    private Long startOsmId;

    @Id
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
        properties.put("startOsmId", this.startOsmId);
        properties.put("endOsmId", this.endOsmId);
        properties.put("trafficSignalClusterId", this.trafficSignalClusterId);
        properties.put("startName", this.startName);
        properties.put("endName", this.endName);
        properties.put("streetNames", this.streetNames);
        return properties;
    }
}
