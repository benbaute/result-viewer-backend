package com.simra.konsumgandalf.valhalla.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValhallaMatchedPoint {

    Double lat;
    Double lon;

    @JsonProperty("type")
    String matchingResult;

    @JsonProperty("edge_index")
    @JsonDeserialize(using = EdgeIndexDeserializer.class)
    Integer edgeIndex;

    @JsonProperty("distance_along_edge")
    Double distanceAlongEdge;

    @JsonProperty("distance_from_trace_point")
    Double distanceFromTracePoint;

    public ValhallaMatchedPoint() {
    }

    public ValhallaMatchedPoint(ValhallaMatchedPoint other, Integer edgeIndexOffset) {
        this.lat = other.lat;
        this.lon = other.lon;
        this.matchingResult = other.matchingResult;
        this.edgeIndex = other.edgeIndex != null ? other.edgeIndex + edgeIndexOffset : null;
        this.distanceAlongEdge = other.distanceAlongEdge;
        this.distanceFromTracePoint = other.distanceFromTracePoint;
    }
}
