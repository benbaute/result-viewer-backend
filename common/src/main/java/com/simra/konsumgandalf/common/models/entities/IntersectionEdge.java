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
public class IntersectionEdge extends IntersectionBase implements FeatureMappable {

    private Long valhallaEdgeId;
    private Long nextValhallaEdgeId;
    private Long prevValhallaEdgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "osm_id")
    private PlanetOsmLine osmLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_osm_id")
    private PlanetOsmLine nextOsmLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_osm_id")
    private PlanetOsmLine prevOsmLine;

	public IntersectionEdge() {
	}

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        properties.put("valhallaEdgeId", valhallaEdgeId);
        properties.put("nextValhallaEdgeId", nextValhallaEdgeId);
        properties.put("prevValhallaEdgeId", prevValhallaEdgeId);
        properties.put("osmId", osmLine != null ? osmLine.getId() : null);
        properties.put("prevOsmId", prevOsmLine != null ? prevOsmLine.getId() : null);
        properties.put("nextOsmId", nextOsmLine != null ? nextOsmLine.getId() : null);
        properties.put("name", osmLine != null ? osmLine.getName() : null);
        return properties;
    }
}
