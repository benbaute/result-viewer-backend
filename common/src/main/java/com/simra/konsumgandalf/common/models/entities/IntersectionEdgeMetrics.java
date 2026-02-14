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
public class IntersectionEdgeMetrics extends IntersectionBaseClassMetrics implements FeatureMappable {
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
        properties.put("osmId", osmId);
        properties.put("prevOsmId", prevOsmId);
        properties.put("nextOsmId", nextOsmId);
        properties.put("name", name);
        return properties;
    }
}
