package com.simra.konsumgandalf.common.models.classes;

import com.simra.konsumgandalf.common.models.entities.TimeBaseClass;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntersectionEdgeMetricsIDKey extends TimeBaseClass {
	private Long osmId;
    private Long prevOsmId;
    private Long nextOsmId;
}
