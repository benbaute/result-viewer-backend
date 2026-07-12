package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an administrative region like a state or a city.
 */
@Getter
@Setter
@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
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

	// Self referencing column, with parent region of lower admin level
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Region parent;

	@Column(name = "ltree_path", columnDefinition = "ltree")
	private String ltreePath;

	public Region() {
	}

	@Override
	public Polygon getGeom() {
		return way;
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("id", id);
		properties.put("name", name);
		properties.put("adminLevel", adminLevel);
		properties.put("ltreePath", ltreePath);
		return properties;
	}

}
