package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;


import java.util.*;

@Getter
@Setter
@Entity
public class TrafficSignal implements FeatureMappable {

	@Id
	private Long id;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

    // For accurate comparison in meters
    @Column(columnDefinition = "geometry(Point,25833)")
    private Point geom25833;

	public TrafficSignal() {
	}
    public TrafficSignal(long id, Point point) {
        this.id = id;
        this.geom = point;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", this.getId());
        return properties;
    }
}
