package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLHStoreType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(indexes = { @Index(columnList = "osm_id")})
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
    @OneToMany(mappedBy = "planetOsmLine")
    private List<SafetyMetricsPlanetOsmLine> safetyMetricPlanetOsmLines;

	/**
	 * The incidents that occurred on this street
	 */
	@OneToMany
	@JoinColumn(name = "planet_osm_line_osm_id")
	private List<RideIncident> rideIncident;

	@Column
	private Geometry way;

	@Column
	private String highway;

    @ManyToMany(mappedBy = "planetOsmLines", fetch = FetchType.LAZY)
	@JsonIgnore
	private Set<RideEntity> rideEntities;

	@Column
	private String name;

	@Type(PostgreSQLHStoreType.class)
	@Column(columnDefinition = "hstore")
	private Map<String, String> tags = new HashMap<>();

	public PlanetOsmLine() {
	}
}
