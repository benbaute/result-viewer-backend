package com.simra.konsumgandalf.valhalla.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TraceResponse {

	List<ValhallaEdge> edges;

    @JsonProperty("matched_points")
    List<ValhallaMatchedPoint> matchedPoints;

    List<MatchInformation> payloadCoordinates;

    public TraceResponse(List<ValhallaEdge> edges, List<ValhallaMatchedPoint> matchedPoints,
                         List<MatchInformation> payloadCoordinates) {
        this.edges = edges;
        this.matchedPoints = matchedPoints;
        this.payloadCoordinates = payloadCoordinates;
    }
}
