package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from intersection_edge_metrics")
public class IntersectionEdgeMetrics extends IntersectionBaseMetrics implements FeatureMappable {

    @Column(name = "valhalla_edge_id")
    private Long valhallaEdgeId;

    @Column(name = "next_valhalla_edge_id")
    private Long nextValhallaEdgeId;

    @Column(name = "prev_valhalla_edge_id")
    private Long prevValhallaEdgeId;

    @Column(name = "osm_id")
    private Long osmId;

    @Column(name = "prev_osm_id")
    private Long prevOsmId;

    @Column(name = "next_osm_id")
    private Long nextOsmId;

    @Column(name = "name")
    private String name;

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        properties.put("valhallaEdgeId", valhallaEdgeId);
        properties.put("nextValhallaEdgeId", nextValhallaEdgeId);
        properties.put("prevValhallaEdgeId", prevValhallaEdgeId);
        properties.put("osmId", osmId);
        properties.put("prevOsmId", prevOsmId);
        properties.put("nextOsmId", nextOsmId);
        properties.put("name", name);
        return properties;
    }
}
