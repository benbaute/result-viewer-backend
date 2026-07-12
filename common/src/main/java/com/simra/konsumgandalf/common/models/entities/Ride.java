package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.interfaces.Identifiable;
import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Contains metadata for a ride.
 */
@Getter
@Setter
@Entity
public class Ride implements Identifiable, PropertiesMappable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String path;

	@ManyToMany
	@JoinTable(name = "ride__region", joinColumns = @JoinColumn(name = "ride_id"),
			inverseJoinColumns = @JoinColumn(name = "region_id"))
	private Set<Region> regions = new HashSet<>();

	@Transient
	private ArrayList<MatchInformation> coordinates;

	public Ride() {
	}

	public Ride(String path) {
		this.path = path;
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("id", id);
		properties.put("path", path);
		return properties;
	}

}
