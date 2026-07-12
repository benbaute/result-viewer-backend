package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.locationtech.jts.geom.Polygon;

import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "intersection_region_metrics")
public class IntersectionRegionMetrics extends IntersectionRegionBaseMetrics implements FeatureMappable {

	@Column(name = "number_of_rides")
	private int numberOfRides;

	public Polygon getGeom() {
		return this.getRegion() != null ? this.getRegion().getGeom() : null;
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = this.getRegionBaseProperties();
		properties.put("id", this.getRegion().getId());
		properties.put("ltreePath", this.getRegion().getLtreePath());
		properties.put("name", this.getRegion().getName());
		properties.put("adminLevel", this.getRegion().getAdminLevel());
		properties.put("numberOfRides", numberOfRides);

		return properties;
	}

}
