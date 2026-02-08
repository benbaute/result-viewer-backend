package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", this.getId());
        return properties;
    }
}
