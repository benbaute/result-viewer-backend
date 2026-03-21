package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionBase;
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

	public static <T extends IntersectionBaseMetrics> Specification<T> hasRegionId(Long regionId) {
		return (root, query, cb) -> {
			if (regionId == null) {
				return cb.conjunction();
			}

			assert query != null;
			Subquery<Long> subquery = query.subquery(Long.class);
			Root<IntersectionBase> base = subquery.from(IntersectionBase.class);

			Join<IntersectionBase, Region> regionJoin = base.join("regions");

			subquery.select(cb.literal(1L))
				.where(cb.equal(base.get("id"), root.get("exampleId")), cb.equal(regionJoin.get("id"), regionId));

			return cb.exists(subquery);
		};
	}

	public static <T extends IntersectionBaseMetrics> Specification<T> hasRegion(String regionName) {
		return (root, query, cb) -> {
			if (regionName == null) {
				return cb.conjunction();
			}

			assert query != null;
			Subquery<Long> subquery = query.subquery(Long.class);
			Root<IntersectionBase> base = subquery.from(IntersectionBase.class);

			Join<IntersectionBase, Region> regionJoin = base.join("regions");

			subquery.select(cb.literal(1L))
				.where(cb.equal(base.get("id"), root.get("exampleId")), cb.equal(regionJoin.get("name"), regionName));

			return cb.exists(subquery);
		};
	}

}
