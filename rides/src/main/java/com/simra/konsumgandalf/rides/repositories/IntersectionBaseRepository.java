package com.simra.konsumgandalf.rides.repositories;


import com.simra.konsumgandalf.common.models.entities.IntersectionBase;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntersectionBaseRepository extends JpaRepository<IntersectionBase, Long> {
    @Query(value = """
    SELECT base.ride.id
    FROM IntersectionBase base
    WHERE base.id = :intersectionBaseId
""")
    Optional<Long> findRideIdByIntersectionBaseId(Long intersectionBaseId);

    @Query(value = """
SELECT base
FROM IntersectionBase base
WHERE (:id IS NULL OR base.id = :id)
AND (:weekDay = 'ALL_WEEK' OR base.weekDay = :weekDay)
AND (:trafficTime = 'ALL_DAY' OR base.trafficTime = :trafficTime)
AND (:year = 2000 OR base.year = :year)
""")
    List<IntersectionBase> getIntersectionBaseAggregateDate(
            @Param("id") Long id,
            @Param("trafficTime") TrafficTimes trafficTime,
            @Param("weekDay") WeekDays weekDay,
            @Param("year") Integer year
    );

    @Query(value = """
SELECT base
FROM IntersectionBase base
WHERE (:id IS NULL OR base.id = :id)
AND base.startTime >= :startDate
AND base.endTime <= :endDate
""")
    List<IntersectionBase> getIntersectionBaseStartEnd(
            @Param("id") Long id,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}


