package com.simra.konsumgandalf.common.models.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Window;

import java.util.Map;

public interface PropertiesMappable {

	Map<String, Object> getProperties();

	static Map<String, Object> toPageableMap(Page<?> page, String key, Object value) {
		return Map.of("metadata", Map.of("totalElements", page.getTotalElements(), "totalPages", page.getTotalPages(),
				"currentPage", page.getNumber()), key, value);
	}

	static Map<String, Object> toPropertiesCollection(Page<? extends PropertiesMappable> page) {
		return toPageableMap(page, "properties", page.stream().map(PropertiesMappable::getProperties).toList());
	}

	private static Map<String, Object> toWindowMap(Window<?> window, Long lastId, Object value) {
		return Map.of("metadata", Map.of("hasNext", window.hasNext(), "lastId", lastId != null ? lastId : ""),
				"properties", value);
	}

	static Map<String, Object> toPropertiesCollection(Window<? extends PropertiesMappable> window, Long lastId) {
		return toWindowMap(window, lastId, window.stream().map(PropertiesMappable::getProperties).toList());
	}

}
