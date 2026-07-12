package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionBaseSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionEdgeSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.TimeSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface IntersectionEdgeRepository extends RepositoryFeatureMappable<IntersectionEdge, Long> {

	@Override
	@NonNull
	@EntityGraph(attributePaths = { "osmLine", "nextOsmLine", "prevOsmLine", })
	Page<IntersectionEdge> findAll(Specification<IntersectionEdge> spec, @NonNull Pageable pageable);

	@Query(value = """
				SELECT e FROM IntersectionEdge e
				JOIN FETCH e.osmLine
			    JOIN FETCH e.prevOsmLine
				JOIN FETCH e.nextOsmLine
			    WHERE e.ride.id = :rideId
			""")
	List<IntersectionEdge> findByRideId(Long rideId);

	@Query(value = """
			SELECT DISTINCT name
			FROM intersection_edge_metrics
			WHERE EXISTS (
			             SELECT 1
			             FROM region
			             WHERE region.ltree_path <@ (:targetLtreePath)::ltree
			)
			AND name ILIKE CONCAT('%', :name, '%')
			AND name IS NOT NULL
			LIMIT 1000
			""", nativeQuery = true)
	List<String> findAllStreetNames(String targetLtreePath, String name);

	default Specification<IntersectionEdge> createSpecification(Long osmId, Long valhallaEdgeId,
			Long prevValhallaEdgeId, Long nextValhallaEdgeId, String regionLTreePath, TrafficTimes trafficTime,
			WeekDays weekDay, Integer year, Date startDate, Date endDate, boolean countQuery) {
		Specification<IntersectionEdge> spec = Specification.where(IntersectionEdgeSpecifications.hasOsmId(osmId))
			.and(IntersectionEdgeSpecifications.isSegment(valhallaEdgeId, prevValhallaEdgeId, nextValhallaEdgeId))
			.and(IntersectionBaseSpecifications.isInsideRegionPath(regionLTreePath))
			.and(TimeSpecifications.hasWeekDayBase(weekDay))
			.and(TimeSpecifications.hasTrafficTimeBase(trafficTime))
			.and(TimeSpecifications.hasYearBase(year))
			.and(TimeSpecifications.inDateRange(startDate, endDate));
		if (!countQuery) {
			return spec.and(IntersectionEdgeSpecifications.fetchOsmLines());
		}
		return spec;
	}

}
