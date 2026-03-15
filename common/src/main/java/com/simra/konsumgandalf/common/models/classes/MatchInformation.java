package com.simra.konsumgandalf.common.models.classes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * This class is used to store the information of the matched point from the OSRM service
 */
@Setter
@Getter
public class MatchInformation extends Coordinate implements Serializable {

	@JsonProperty("time")
	private long timestamp; // in seconds

	private double accuracy;

	@JsonIgnore
	private Long ridePointId;

	public MatchInformation(double lng, double lat, long time, double accuracy) {
		super(lng, lat);
		this.timestamp = time;
		this.accuracy = accuracy;
	}

	public MatchInformation() {
	}

}
