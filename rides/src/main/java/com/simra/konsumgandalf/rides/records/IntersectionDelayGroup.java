package com.simra.konsumgandalf.rides.records;



public interface IntersectionDelayGroup {
    Long getStartLineId();
    Long getEndLineId();
    Long getCount();
    Double getAvgLength();
    Double getAvgDuration();
    Double getMaxDuration();
    Double getAvgSpeed();
    String getExampleGeom();
}

