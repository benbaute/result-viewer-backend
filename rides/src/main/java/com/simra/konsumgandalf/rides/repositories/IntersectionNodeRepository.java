package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionBaseSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionNodeSpecifications;
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
public interface IntersectionNodeRepository extends RepositoryFeatureMappable<IntersectionNode, Long> {

	@Override
	@NonNull
	@EntityGraph(attributePaths = { "startOsmLine", "endOsmLine", "trafficSignalCluster" })
	Page<IntersectionNode> findAll(Specification<IntersectionNode> spec, @NonNull Pageable pageable);

	@Query(value = """
			    SELECT n FROM IntersectionNode n
			    JOIN FETCH n.startOsmLine
			    JOIN FETCH n.endOsmLine
			    WHERE n.ride.id = :rideId
			""")
	List<IntersectionNode> findByRideId(Long rideId);

	@Query(value = """
			SELECT DISTINCT street_names
			FROM intersection_node_metrics
			WHERE EXISTS (
			             SELECT 1
			             FROM region
			             WHERE region.ltree_path <@ (:targetLtreePath)::ltree
			)
			AND street_names ILIKE CONCAT('%', :streetNames, '%')
			AND street_names IS NOT NULL
			LIMIT 1000
			""", nativeQuery = true)
	List<String> findAllIncludingString(String targetLtreePath, String streetNames);

	default Specification<IntersectionNode> createSpecification(Long trafficSignalClusterId, Long startValhallaEdgeId,
			Long endValhallaEdgeId, String regionLTreePath, TrafficTimes trafficTime, WeekDays weekDay, Integer year,
			Date startDate, Date endDate, boolean countQuery) {
		Specification<IntersectionNode> spec = Specification
			.where(IntersectionNodeSpecifications.hasTrafficSignalClusterId(trafficSignalClusterId))
			.and(IntersectionNodeSpecifications.isSegment(startValhallaEdgeId, endValhallaEdgeId))
			.and(IntersectionBaseSpecifications.isInsideRegionPath(regionLTreePath))
			.and(TimeSpecifications.hasWeekDayBase(weekDay))
			.and(TimeSpecifications.hasTrafficTimeBase(trafficTime))
			.and(TimeSpecifications.hasYearBase(year))
			.and(TimeSpecifications.inDateRange(startDate, endDate));
		if (!countQuery) {
			return spec.and(IntersectionNodeSpecifications.fetchOsmLines());
		}
		return spec;
	}

}
