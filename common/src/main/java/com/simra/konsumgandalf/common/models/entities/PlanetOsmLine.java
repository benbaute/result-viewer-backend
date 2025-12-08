package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLHStoreType;
import org.hibernate.annotations.Type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Geometry;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity()
@Table(indexes = { @Index(columnList = "osm_id"),
		@Index(columnList = "lastModified, lastAnalysed", name = "idx_last_modified_last_analysis") })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PlanetOsmLine {

	@Id
	@Column(name = "osm_id", unique = true)
	private long id;

	/**
	 * The metrics indicating the safety of this street segment. Therefore, the
	 * {@link #rideIncident} field is used to calculate the metrics.
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "planetOsmLine", orphanRemoval = true)
	private List<SafetyMetricsPlanetOsmLine> safetyMetricPlanetOsmLines;

	/**
	 * The incidents that occurred on this street
	 */
	@OneToMany(cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "planet_osm_line_osm_id")
	private List<RideIncident> rideIncident;

	@Column
	private Geometry way;

	@Column
	private String highway;

	@ManyToMany(cascade = { CascadeType.DETACH, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.PERSIST },
			fetch = FetchType.LAZY, mappedBy = "planetOsmLines")
	@JsonIgnore
	private Set<RideEntity> rideEntities;

	@Column
	private String name;

	@Type(PostgreSQLHStoreType.class)
	@Column(columnDefinition = "hstore")
	private Map<String, String> tags = new HashMap<>();

	@Column(nullable = true)
	private Instant lastModified;

	@Column(nullable = true)
	private Instant lastAnalysed;

	public PlanetOsmLine() {
	}

	public PlanetOsmLine(long id, Geometry way) {
		this.id = id;
		this.way = way;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Geometry getWay() {
		return way;
	}

	public void setWay(Geometry way) {
		this.way = way;
	}

	public String getHighway() {
		return highway;
	}

	public void setHighway(String highway) {
		this.highway = highway;
	}

	public List<RideIncident> getRideIncident() {
		return rideIncident;
	}

	public void setRideIncident(List<RideIncident> rideIncident) {
		this.rideIncident = rideIncident;
	}

	public void setRideIncident(RideIncident rideIncident) {
		this.getRideIncident().add(rideIncident);
	}

	public List<SafetyMetricsPlanetOsmLine> getSafetyMetrics() {
		return safetyMetricPlanetOsmLines;
	}

	public void setSafetyMetrics(List<SafetyMetricsPlanetOsmLine> safetyMetricPlanetOsmLines) {
		this.safetyMetricPlanetOsmLines = safetyMetricPlanetOsmLines;
	}

	public Set<RideEntity> getRideEntities() {
		return rideEntities;
	}

	public void setRideEntities(Set<RideEntity> rideEntities) {
		this.rideEntities = rideEntities;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public List<SafetyMetricsPlanetOsmLine> getSafetyMetricPlanetOsmLines() {
		return safetyMetricPlanetOsmLines;
	}

	public void setSafetyMetricPlanetOsmLines(List<SafetyMetricsPlanetOsmLine> safetyMetricPlanetOsmLines) {
		this.safetyMetricPlanetOsmLines = safetyMetricPlanetOsmLines;
	}

	public Instant getLastModified() {
		return lastModified;
	}

	public void setLastModified(Instant lastModified) {
		this.lastModified = lastModified;
	}

	public Instant getLastAnalysed() {
		return lastAnalysed;
	}

	public void setLastAnalysed(Instant lastAnalysed) {
		this.lastAnalysed = lastAnalysed;
	}

}
