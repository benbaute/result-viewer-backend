package com.simra.konsumgandalf.common.models.dtos;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class IntersectionEdgeAggregate implements FeatureMappable {
    private Geometry geom;
    private Map<String, Object> properties;

    public IntersectionEdgeAggregate(
            Geometry geom,
            Long exampleId,
            Long osmId,
            Long prevOsmId,
            Long nextOsmId,
            String name,
            Long count,
            Double avgLength,
            Double avgDuration,
            Double maxDuration,
            Double medianDuration,
            Double avgSpeed,
            Double medianSpeed,
            Double avgWaitingTime,
            Double maxWaitingTime,
            Double medianWaitingTime
    ) {
        this.geom = geom;
        Map<String, Object> props = new HashMap<>();
        props.put("example_id", exampleId);
        props.put("osm_id", osmId);
        props.put("prev_osm_id", prevOsmId);
        props.put("next_osm_id", nextOsmId);
        props.put("name", name);
        props.put("count", count);
        props.put("avg_length", avgLength);
        props.put("avg_duration", avgDuration);
        props.put("max_duration", maxDuration);
        props.put("median_duration", medianDuration);
        props.put("avg_speed", avgSpeed);
        props.put("median_speed", medianSpeed);
        props.put("avg_waiting_time", avgWaitingTime);
        props.put("max_waiting_time", maxWaitingTime);
        props.put("median_waiting_time", medianWaitingTime);

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
