package com.simra.konsumgandalf.common.models.classes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * This class is used to store the information of the matched point from the OSRM service
 */
@Getter
@Setter
public class MatchInformationDate extends Coordinate implements Serializable {

	@JsonProperty("time")
    private long valhallaTimestamp;;

    @JsonIgnore
    private Date originalTimestamp;

	public MatchInformationDate(double lng, double lat, Date originalTimestamp, long valhallaTimestamp) {
		super(lng, lat);
		this.originalTimestamp = originalTimestamp;
        this.valhallaTimestamp = valhallaTimestamp;
	}

	public MatchInformationDate() {
	}
}
