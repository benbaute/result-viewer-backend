package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

	@Modifying
	@Transactional
	@Query(value = """
			TRUNCATE TABLE intersection_edge CASCADE;
			TRUNCATE TABLE intersection_node CASCADE;
			TRUNCATE TABLE matched_point;
			TRUNCATE TABLE ride_point CASCADE;
			TRUNCATE TABLE ride CASCADE;

			""", nativeQuery = true)
	void truncateAllRideTables();

	@Query(value = """
			    SELECT id FROM Ride
			""")
	List<Long> getRideIds();


    @Query(value = """
		SELECT edges.ride_id,
		edges.sum_length AS edges_length,
		edges.sum_duration AS edges_duration,
		edges.count AS edges_count,
		nodes.sum_length AS delays_length,
		nodes.sum_duration AS delays_duration,
		nodes.count AS delays_count,
		nodes.sum_duration / (nodes.sum_duration + edges.sum_duration) AS nodes_duration_portion,
		nodes.sum_length / (nodes.sum_length + edges.sum_length) AS nodes_length_portion,
		(edges.avg_speed - nodes.avg_speed) / edges.avg_speed AS relative_speed_loss,
		edges.sum_waiting_time AS edges_wait_sum,
		nodes.sum_waiting_time AS nodes_wait_sum,
		nodes.sum_waiting_time / (edges.sum_duration + nodes.sum_duration - nodes.sum_waiting_time) AS rel_wait
		FROM
		(SELECT
            ride_id,
            COUNT(*) AS count,
            SUM(length) AS sum_length,
            SUM(duration) AS sum_duration,
		    SUM(length) / SUM(duration) AS avg_speed,
			SUM(waiting_time) AS sum_waiting_time
        FROM intersection_edge
        GROUP BY ride_id ) edges,
		(SELECT
            ride_id,
            COUNT(*) AS count,
            SUM(length) AS sum_length,
            SUM(duration) AS sum_duration,
		    SUM(length) / SUM(duration) AS avg_speed,
			SUM(waiting_time) AS sum_waiting_time
        FROM intersection_node
        GROUP BY ride_id ) nodes
		WHERE edges.ride_id = nodes.ride_id
		ORDER BY rel_wait DESC
		""", nativeQuery = true)
    List<Long> getRideAggregate();

    @Query(value = """
		SELECT
			nodes.id,
			nodes.ride_id,
			nodes.duration,
			nodes.length,
			nodes.length / edges.avg_speed AS expectedTime,
			(nodes.length / edges.avg_speed) - duration AS waitTime
		
		FROM
		(SELECT
            ride_id,
            COUNT(*) AS count,
            SUM(length) AS sum_length,
            SUM(duration) AS sum_duration,
		    SUM(length) / SUM(duration) AS avg_speed
        FROM intersection_edge
		WHERE ride_id = 7455
        GROUP BY ride_id ) edges,
		(SELECT
            ride_id,
		    id,
            length,
		    duration
        FROM intersection_node
		WHERE ride_id = 7455
        ) nodes
		WHERE edges.ride_id = nodes.ride_id
		""", nativeQuery = true)
    List<Long> calculateWaitTimes();


    @Query(value = """

            WITH edges AS (
      SELECT
          region_name,
          SUM(length)   AS edge_length,
          SUM(duration) AS edge_duration
      FROM intersection_edge
      GROUP BY region_name
  ),
  nodes AS (
      SELECT
          region_name,
          SUM(length)   AS node_length,
          SUM(duration) AS node_duration
      FROM intersection_node
      GROUP BY region_name
  )
  SELECT
      e.region_name,
  
      -- raw totals
      e.edge_length,
      e.edge_duration,
      n.node_length,
      n.node_duration,
  
      -- time-based delay share (recommended)
      n.node_duration
        / (n.node_duration + e.edge_duration)
        AS delay_time_fraction,
  
      -- distance-based delay share
      n.node_length
        / (n.node_length + e.edge_length)
        AS delay_distance_fraction,
  
      -- effective speeds
      e.edge_length / e.edge_duration AS free_flow_speed,
      (e.edge_length + n.node_length)
        / (e.edge_duration + n.node_duration) AS effective_speed,
  
      -- relative speed loss
      1 - (
          (e.edge_length + n.node_length)
          / (e.edge_duration + n.node_duration)
        ) / (e.edge_length / e.edge_duration)
        AS relative_speed_loss
  
  FROM edges e
  JOIN nodes n
    ON e.region_name = n.region_name
  ORDER BY relative_speed_loss DESC;
		""", nativeQuery = true)
    List<Long> getRegionAggregate();
}
