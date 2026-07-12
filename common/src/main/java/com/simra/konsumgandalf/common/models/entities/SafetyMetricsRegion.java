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
@Table(name = "safety_metrics__region")
public class SafetyMetricsRegion extends SafetyMetrics<SafetyMetricsRegion> {

	@Column(name = "id")
	private Long id;

	@Id // This is not really an id column, but else the specification fails
	@Column(name = "name")
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id", insertable = false, updatable = false)
	private Region region;

	@Column(name = "total_distance")
	private Float totalDistance;

	public SafetyMetricsRegion() {
		super();
	}

}
