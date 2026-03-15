package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * This entity represents the safety metrics of an object
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class SafetyMetrics<T extends SafetyMetrics<T>> {

	@Id
	@Column(name = "traffic_time", length = 21)
	@Enumerated(EnumType.STRING)
	private TrafficTimes trafficTime;

	@Id
	@Column(name = "week_day", length = 12)
	@Enumerated(EnumType.STRING)
	private WeekDays weekDay;

	@Id
	@Column(name = "year")
	private Integer year;

	@Column(name = "dangerous_color", length = 7)
	private String dangerousColor;

	// The calculated risk to the rider to drive on this path
	@Column(name = "dangerous_score")
	private float dangerousScore;

	@Column(name = "number_of_rides")
	private int numberOfRides;

	@Column(name = "number_of_incidents")
	private int numberOfIncidents;

	@Column(name = "number_of_scary_incidents")
	private int numberOfScaryIncidents;

	@Column(name = "number_of_close_passes")
	private int numberOfClosePasses;

	@Column(name = "number_of_pull_in_outs")
	private int numberOfPullInOuts;

	@Column(name = "number_of_near_left_right_hooks")
	private int numberOfNearLeftRightHooks;

	@Column(name = "number_of_head_on_approaches")
	private int numberOfHeadOnApproaches;

	@Column(name = "number_of_tailgating")
	private int numberOfTailgating;

	@Column(name = "number_of_near_doorings")
	private int numberOfNearDoorings;

	@Column(name = "number_of_obstacle_dodges")
	private int numberOfObstacleDodges;

	public SafetyMetrics() {
	}

}
