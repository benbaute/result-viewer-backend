package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.common.models.interfaces.Identifiable;
import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Map;

@NoRepositoryBean
public interface RepositoryFeatureMappable<T extends Identifiable & FeatureMappable, ID>
		extends RepositoryPropertiesMappable<T, ID> {

	default Map<String, Object> fetchPageCollection(Specification<T> spec, Pageable pageable, Boolean toProperties) {
		Page<T> page = fetchPage(spec, pageable);
		if (toProperties) {
			return PropertiesMappable.toPropertiesCollection(page);
		}
		return FeatureMappable.toFeatureCollection(page);
	}

}