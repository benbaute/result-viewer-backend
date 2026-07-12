package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.classes.SafetyMetricsNameKey;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * This entity represents the safety metrics of a region.
 */
@Getter
@Entity
@IdClass(SafetyMetricsNameKey.class)
@org.hibernate.annotations.Immutable
@Table(name = "safety_metrics__simra_region")
public class SafetyMetricsSimraRegion extends SafetyMetrics<SafetyMetricsSimraRegion> {

	@Id
	@Column(name = "name")
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "name", insertable = false, updatable = false)
	private SimraRegion region;

	@Column(name = "total_distance")
	private Float totalDistance;

	public SafetyMetricsSimraRegion() {
		super();
	}

}
