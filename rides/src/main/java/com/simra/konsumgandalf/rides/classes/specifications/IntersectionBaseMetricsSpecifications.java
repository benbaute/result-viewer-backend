package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionBaseMetrics;
import com.simra.konsumgandalf.common.models.entities.Region;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionBaseMetricsSpecifications {

	public static <T> Specification<T> hasMinCount(Long count) {
		return (root, query, cb) -> count == null ? cb.conjunction()
				: cb.greaterThanOrEqualTo(root.get("numberOfRides"), count);
	}

	public static <T extends IntersectionBaseMetrics> Specification<T> hasRegion(String regionName) {
		return (root, query, cb) -> {
			if (regionName == null) {
				return cb.conjunction();
			}

			assert query != null;
			Subquery<Long> subquery = query.subquery(Long.class);
			Root<IntersectionBaseMetrics> base = subquery.from(IntersectionBaseMetrics.class);

			Join<IntersectionBaseMetrics, Region> region = base.join("regions");

			subquery.select(cb.literal(1L))
				.where(cb.equal(base.get("id"), root.get("exampleId")),
						// TODO: replace with region id, requires frontend changes
						cb.equal(region.get("name"), regionName));

			return cb.exists(subquery);
		};
	}

}
