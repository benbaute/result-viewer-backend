package com.simra.konsumgandalf.rides.repositories;


import com.simra.konsumgandalf.common.models.entities.IntersectionBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntersectionBaseRepository extends JpaRepository<IntersectionBase, Long> {
    @Query(value = """
    SELECT base.ride.id
    FROM IntersectionBase base
    WHERE base.id = :intersectionBaseId
""")
    Optional<Long> findRideIdByIntersectionBaseId(Long intersectionBaseId);
}


