package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

/**
 * Contains multiple regions to a super region.
 */
@Getter
@Setter
@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class SimraRegion {

	@Id
	@Column
	private String name;

	// The regions should be from the same admin level and MUST not overlap or else
	// calculations based on them are messed up
	// Example: In case of berlin-latest.osm.pbf, SimraRegion "All" is wrong by times 2 as
	// it is constructed by Brandenburg and Berlin,
	// but Brandenburg is the same Polygon as Berlin for berlin-latest.osm.pbf
	@ManyToMany
	@JoinTable(name = "simra_region__region",
			joinColumns = @JoinColumn(name = "simra_region_name", referencedColumnName = "name"),
			inverseJoinColumns = @JoinColumn(name = "region_id", referencedColumnName = "id"))
	@JsonManagedReference
	private List<Region> regions;

	@Column(columnDefinition = "geometry(Polygon,4326)")
	private Polygon way;

	public SimraRegion() {
	}

}
