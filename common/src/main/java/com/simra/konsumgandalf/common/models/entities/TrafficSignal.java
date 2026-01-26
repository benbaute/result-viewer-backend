package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;


import java.util.*;

@Getter
@Setter
@Entity
public class TrafficSignal implements FeatureMappable {

	@Id
	private Long id;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

    @ManyToMany
    @JoinTable(
            name = "traffic_signal__planet_osm_line",
            joinColumns = @JoinColumn(name = "traffic_signal_id"),
            inverseJoinColumns = @JoinColumn(name = "osm_line_id")
    )
    private Set<PlanetOsmLine> osmLines = new HashSet<>();

	public TrafficSignal() {
	}
    public TrafficSignal(long id, Point point) {
        this.id = id;
        this.geom = point;
    }

    public void addOsmLine(PlanetOsmLine osmLine) {
        osmLines.add(osmLine);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", this.getId());
        return properties;
    }
}
