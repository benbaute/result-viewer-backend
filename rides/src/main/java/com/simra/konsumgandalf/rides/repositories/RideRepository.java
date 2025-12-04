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
			TRUNCATE TABLE intersection_delay;
			TRUNCATE TABLE edge;
			TRUNCATE TABLE matched_point;
			TRUNCATE TABLE ride_point CASCADE;
			TRUNCATE TABLE ride CASCADE;

			""", nativeQuery = true)
	void truncateAllRideTables();

	@Query(value = """
			    SELECT id FROM Ride
			""")
	List<Long> getRideIds();

}
