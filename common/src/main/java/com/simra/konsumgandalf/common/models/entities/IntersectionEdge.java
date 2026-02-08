package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.dtos.IntersectionEdgeAggregate;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Getter
@Setter
@Entity
@SqlResultSetMapping(
    name = "IntersectionEdgeAggregateMapping",
    classes = @ConstructorResult(
        targetClass = IntersectionEdgeAggregate.class,
        columns = {
            @ColumnResult(name = "geom", type = LineString.class),
            @ColumnResult(name = "example_id", type = Long.class),
            @ColumnResult(name = "osm_id", type = Long.class),
            @ColumnResult(name = "prev_osm_id", type = Long.class),
            @ColumnResult(name = "next_osm_id", type = Long.class),
            @ColumnResult(name = "name", type = String.class),
            @ColumnResult(name = "count", type = Long.class),
            @ColumnResult(name = "avg_length", type = Double.class),
            @ColumnResult(name = "avg_duration", type = Double.class),
            @ColumnResult(name = "max_duration", type = Double.class),
            @ColumnResult(name = "median_duration", type = Double.class),
            @ColumnResult(name = "avg_speed", type = Double.class),
            @ColumnResult(name = "median_speed", type = Double.class),
            @ColumnResult(name = "avg_waiting_time", type = Double.class),
            @ColumnResult(name = "max_waiting_time", type = Double.class),
            @ColumnResult(name = "median_waiting_time", type = Double.class)
        }
    )
)
@NamedNativeQuery(
    name = "IntersectionEdge.aggregateEdges",
    query = """
    SELECT
        example.geom AS geom,
        aggregate.example_id AS example_id,
        aggregate.osm_id,
        aggregate.prev_osm_id,
        aggregate.next_osm_id,
        line.name AS name,
        aggregate.count,
        aggregate.avg_length,
        aggregate.avg_duration,
        aggregate.max_duration,
        aggregate.median_duration,
        aggregate.avg_speed,
        aggregate.median_speed,
        aggregate.avg_waiting_time,
        aggregate.max_waiting_time,
        aggregate.median_waiting_time
    FROM (
        SELECT
            osm_id,
            prev_osm_id,
            next_osm_id,
            COUNT(*) AS count,
            AVG(length) AS avg_length,
            AVG(duration) AS avg_duration,
            MAX(duration) AS max_duration,
            AVG(speed) AS avg_speed,
            MIN(id) AS example_id,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY speed DESC) AS median_speed,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY duration DESC) AS median_duration,
            AVG(waiting_time) AS avg_waiting_time,
            MAX(waiting_time) AS max_waiting_time,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY waiting_time DESC) AS median_waiting_time
        FROM intersection_edge edge
        WHERE (
                :region IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM intersection_edge__region er
                    JOIN region r ON r.id = er.region_id
                    WHERE er.edge_id = edge.id
                    AND r.name = :region
                )
            )
        GROUP BY osm_id, prev_osm_id, next_osm_id
    ) aggregate
    JOIN intersection_edge example ON example.id = aggregate.example_id
    LEFT JOIN planet_osm_line line ON line.osm_id = example.osm_id
    WHERE (:count IS NULL OR aggregate.count >= :count)
    AND (:name IS NULL OR line.name ILIKE CONCAT('%', :name, '%'))
    """,
    resultSetMapping = "IntersectionEdgeAggregateMapping"
)
public class IntersectionEdge extends IntersectionBaseClass implements FeatureMappable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "osm_id")
    private PlanetOsmLine line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_osm_id")
    private PlanetOsmLine nextLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_osm_id")
    private PlanetOsmLine prevLine;


    @ManyToMany
    @JoinTable(
            name = "intersection_edge__region",
            joinColumns = @JoinColumn(name = "edge_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id")
    )
    private Set<Region> regions = new HashSet<>();


	public IntersectionEdge() {
	}

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        PlanetOsmLine line = this.getLine();
        PlanetOsmLine prevLine = this.getPrevLine();
        PlanetOsmLine nextLine = this.getNextLine();
        properties.put("osm_id", line != null ? line.getId() : null);
        properties.put("prev_osm_id", prevLine != null ? prevLine.getId() : null);
        properties.put("next_osm_id", nextLine != null ? nextLine.getId() : null);
        properties.put("name", line != null ? line.getName() : null);
        return properties;
    }

    @Override
    public Set<Region>  getRegions() {
        return this.regions;
    }
}
