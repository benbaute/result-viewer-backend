package com.simra.konsumgandalf.common.models.interfaces;

import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

public interface FeatureMappable extends PropertiesMappable {

	Geometry getGeom();

	default Map<String, Object> getFeatureMap() {
		Map<String, Object> feature = new HashMap<>();
		feature.put("type", "Feature");
		feature.put("geometry", getGeom());
		feature.put("properties", getProperties());
		return feature;
	}

}
