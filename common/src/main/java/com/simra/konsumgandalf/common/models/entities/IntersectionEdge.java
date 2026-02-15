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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "osm_id")
    private PlanetOsmLine line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_osm_id")
    private PlanetOsmLine nextLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_osm_id")
    private PlanetOsmLine prevLine;


	public IntersectionEdge() {
	}

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        PlanetOsmLine line = this.getLine();
        PlanetOsmLine prevLine = this.getPrevLine();
        PlanetOsmLine nextLine = this.getNextLine();
        properties.put("osmId", line != null ? line.getId() : null);
        properties.put("prevOsmId", prevLine != null ? prevLine.getId() : null);
        properties.put("nextOsmId", nextLine != null ? nextLine.getId() : null);
        properties.put("name", line != null ? line.getName() : null);
        return properties;
    }
}
