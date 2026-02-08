package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

import java.util.*;

@Getter
@Setter
@Entity
public class TrafficSignalCluster implements FeatureMappable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @ManyToMany
    @JoinTable(
            name = "traffic_signal_cluster__planet_osm_line",
            joinColumns = @JoinColumn(name = "traffic_signal_cluster_id"),
            inverseJoinColumns = @JoinColumn(name = "osm_id")
    )
    private Set<PlanetOsmLine> osmLines = new HashSet<>();

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon geom;

    // For spatial joins with planet osm line
    @Column(columnDefinition = "geometry(Polygon,3857)")
    private Polygon geom3857;

    @Column(columnDefinition = "bigint[]")
    private List<Long> originalSignalIds;

    @Column(columnDefinition = "varchar(255)[]")
    private List<String> osmLinesName;

	public TrafficSignalCluster() {
	}

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", this.getId());
        properties.put("originalIds", this.getOriginalSignalIds());
        properties.put("osmLinesName", this.getOsmLinesName());
        return properties;
    }
}
