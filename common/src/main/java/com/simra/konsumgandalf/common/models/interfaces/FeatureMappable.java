package com.simra.konsumgandalf.common.models.interfaces;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
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

	static Map<String, Object> toFeatureCollection(List<? extends FeatureMappable> elements) {
		return Map.of("type", "FeatureCollection", "features",
				elements.stream().map(FeatureMappable::getFeatureMap).toList());
	}

	static Map<String, Object> toFeatureCollection(Page<? extends FeatureMappable> page) {
		return PropertiesMappable.toPageableMap(page, "geoData", toFeatureCollection(page.getContent()));
	}

}
