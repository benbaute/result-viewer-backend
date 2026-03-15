package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.classes.SafetyMetricsIDKey;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * This entity represents the safety metrics of a ride.
 */
@Getter
@Entity
@IdClass(SafetyMetricsIDKey.class)
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from safety_metrics__planet_osm_line")
public class SafetyMetricsPlanetOsmLine extends SafetyMetrics<SafetyMetricsPlanetOsmLine> {

	@Id
	@Column(name = "osm_id")
	private Long osmId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "osm_id", insertable = false, updatable = false)
	private PlanetOsmLine planetOsmLine;

}
