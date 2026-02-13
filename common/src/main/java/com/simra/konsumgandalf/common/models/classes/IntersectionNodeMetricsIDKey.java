package com.simra.konsumgandalf.common.models.classes;

import com.simra.konsumgandalf.common.models.entities.TimeBaseClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntersectionNodeMetricsIDKey extends TimeBaseClass {
	private Long startOsmId;
    private Long endOsmId;
}
