package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.interfaces.Identifiable;
import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Map;

@NoRepositoryBean
public interface RepositoryPropertiesMappable<T extends Identifiable & PropertiesMappable, ID>
		extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

	default Page<T> fetchPage(Specification<T> spec, Pageable pageable) {
		Sort stableSort = pageable.getSort().and(Sort.by("id").ascending());
		Pageable stablePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
		return this.findAll(spec, stablePageable);
	}

	default Map<String, Object> fetchScrollCollection(Specification<T> spec, int pageSize, String lastId) {
		ScrollPosition scrollPosition = (lastId != null) ? ScrollPosition.forward(Map.of("id", lastId))
				: ScrollPosition.keyset();

		Window<T> window = this.findBy(spec, q -> q.sortBy(Sort.by("id")).limit(pageSize).scroll(scrollPosition));

		return PropertiesMappable.toPropertiesCollection(window,
				window.getContent().isEmpty() ? null : window.getContent().getLast().getId());
	}

}