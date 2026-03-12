package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an administrative region like a state or a city.
 */
@Getter
@Setter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Region implements FeatureMappable {
    @Id
    private Long id;

	@Column
	private String name;

	@Column
	private int adminLevel;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon way;

    // For spatial joins with planet osm line
    @Column(columnDefinition = "geometry(Polygon,3857)")
    private Polygon geom3857;

	public Region() {
	}

    @Override
    public Geometry getGeom() {
        return way;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", id);
        properties.put("name", name);
        properties.put("adminLevel", adminLevel);
        return properties;
    }
}
