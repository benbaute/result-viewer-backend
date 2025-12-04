package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.Edge;
import com.simra.konsumgandalf.common.models.entities.MatchedPoint;
import com.simra.konsumgandalf.rides.records.EdgeSpeedStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {

	List<Edge> findByRideId(Long rideId);

    @Query(value = """
				SELECT new com.simra.konsumgandalf.rides.records.EdgeSpeedStats(e.line.id, AVG(e.speed), COUNT(e.speed))
				FROM Edge e
                GROUP BY e.line.id
""")
    List<EdgeSpeedStats> getAvgSpeedByEdge();

    @Query(value = """
    SELECT DISTINCT e.ride.id
    FROM Edge e
    WHERE e.line.id = :osmLineId
""")
    List<Long> findByOsmLineId(Long osmLineId);
}
