package com.simra.konsumgandalf.common.models.dtos;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RegionAggregate implements FeatureMappable {
    private Geometry geom;
    private Map<String, Object> properties;

    public RegionAggregate(
            Geometry geom,
            String name,
            Integer adminLevel,
            Long number_of_rides,
            Double node_median_waiting_time,
            Double length_km,
            Double node_waiting_s_per_km,
            Double node_median_waiting_s_per_km,
            Double edge_waiting_s_per_km,
            Double edge_median_waiting_s_per_km
    ) {
        this.geom = geom;
        Map<String, Object> props = new HashMap<>();
        props.put("name", name);
        props.put("admin_level", adminLevel);
        props.put("number_of_rides", number_of_rides);
        props.put("node_median_waiting_time", node_median_waiting_time);
        props.put("length_km", length_km);
        props.put("node_waiting_s_per_km", node_waiting_s_per_km);
        props.put("node_median_waiting_s_per_km", node_median_waiting_s_per_km);
        props.put("edge_waiting_s_per_km", edge_waiting_s_per_km);
        props.put("edge_median_waiting_s_per_km", edge_median_waiting_s_per_km);

        this.properties = props;
    }

    @Override
    public Geometry getGeom() {
        return geom;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}
