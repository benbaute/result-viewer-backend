package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * GPS coordinate for a ride.
 */
@Getter
@Setter
@Entity
public class RidePoint implements FeatureMappable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
	private Ride ride;

	@Column(columnDefinition = "geometry(Point,4326)", nullable = false)
	private Point geom;


	public RidePoint() {
	}

    public RidePoint(Long id) {
        this.id = id;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", id);
        properties.put("timestamp", this.getTimestamp());
        properties.put("path", this.getRide().getPath());
        properties.put("rideId", this.ride.getId());
        return properties;
    }
}
