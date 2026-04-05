package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntersectionNodeRepository
		extends JpaRepository<IntersectionNode, Long>, JpaSpecificationExecutor<IntersectionNode> {

	@Override
	@NonNull
	@EntityGraph(attributePaths = { "startOsmLine", "endOsmLine", "trafficSignalCluster" })
	Page<IntersectionNode> findAll(Specification<IntersectionNode> spec, @NonNull Pageable pageable);

	List<IntersectionNode> findByRideId(Long rideId);

	// TODO: replace with query on mqt
	@Query(value = """
			SELECT DISTINCT node.street_names
			FROM (
			    SELECT MIN(node.id) AS example_id, Count(*) as count
			    FROM intersection_node node
			    WHERE street_names ILIKE CONCAT('%', :streetNames, '%')
			    AND (:trafficSignalClusterId IS NULL OR :trafficSignalClusterId = traffic_signal_cluster_id)
			    AND (
			            :region IS NULL
			            OR EXISTS (
			                SELECT 1
			                FROM intersection_base base
			                JOIN intersection__region ir ON base.id = ir.intersection_id
			                JOIN region r ON r.id = ir.region_id
			                WHERE base.id = node.id
			                AND r.name = :region
			            )
			        )
			    GROUP BY start_osm_id, end_osm_id
			) AS aggregate
			JOIN intersection_node node ON node.id = aggregate.example_id
			WHERE (:count IS NULL OR aggregate.count >= :count)
			""", nativeQuery = true)
	List<String> findAllIncludingString(Long trafficSignalClusterId, Long count, String region, String streetNames);

}
