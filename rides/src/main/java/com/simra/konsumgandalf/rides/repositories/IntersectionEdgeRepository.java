package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;
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
public interface IntersectionEdgeRepository
		extends JpaRepository<IntersectionEdge, Long>, JpaSpecificationExecutor<IntersectionEdge> {

	@Override
	@NonNull
	@EntityGraph(attributePaths = { "osmLine", "nextOsmLine", "prevOsmLine", })
	Page<IntersectionEdge> findAll(Specification<IntersectionEdge> spec, @NonNull Pageable pageable);

	List<IntersectionEdge> findByRideId(Long rideId);

	@Query(value = """
			SELECT DISTINCT line.name
			FROM (
			    SELECT
			        COUNT(*) AS count,
			        MIN(id) AS example_id
			    FROM intersection_edge edge
			    WHERE (
			            :region IS NULL
			            OR EXISTS (
			                SELECT 1
			                FROM intersection_base base
			                JOIN intersection__region ir ON base.id = ir.intersection_id
			                JOIN region r ON r.id = ir.region_id
			                WHERE base.id = edge.id
			                AND r.name = :region
			            )
			        )
			    GROUP BY osm_id, prev_osm_id, next_osm_id
			) aggregate
			JOIN intersection_edge example ON example.id = aggregate.example_id
			LEFT JOIN planet_osm_line line ON line.osm_id = example.osm_id
			WHERE (:count IS NULL OR aggregate.count >= :count)
			AND (:name IS NULL OR line.name ILIKE CONCAT('%', :name, '%'))
			""", nativeQuery = true)
	List<String> findAllStreetNames(Long count, String region, String name);

}
